"""Render the checked-in boss MIDIs into OGG loops.

Requires a General MIDI SoundFont, libfluidsynth and ffmpeg, supplied by the
caller. No synthesizer, SoundFont or cache is stored in the mod repository.

Example:
  python3 scripts/render_boss_music.py --soundfont /path/to/GeneralUser-GS.sf2
"""

import argparse
import ctypes
from pathlib import Path
import struct
import subprocess
import tempfile
import wave


ROOT = Path(__file__).resolve().parents[1]
MUSIC = ROOT / "music"
SOUNDS = ROOT / "src/main/resources/assets/hbk/sounds/music"
TICKS_PER_BAR = 4 * 480


def read_vlq(data, offset):
    value = 0
    while True:
        byte = data[offset]
        offset += 1
        value = (value << 7) | (byte & 0x7F)
        if not byte & 0x80:
            return value, offset


def read_midi(path):
    data = path.read_bytes()
    if data[:4] != b"MThd":
        raise ValueError(f"Not a MIDI file: {path}")
    length, fmt, count, ticks_per_beat = struct.unpack_from(">IHHH", data, 4)
    if length != 6 or fmt != 1:
        raise ValueError(f"Expected format-1 MIDI: {path}")
    offset = 14
    events = []
    tempos = set()
    end_tick = 0
    for track in range(count):
        if data[offset:offset + 4] != b"MTrk":
            raise ValueError(f"Missing track {track}: {path}")
        size = struct.unpack_from(">I", data, offset + 4)[0]
        stream = data[offset + 8:offset + 8 + size]
        offset += size + 8
        position = 0
        tick = 0
        while position < len(stream):
            delta, position = read_vlq(stream, position)
            tick += delta
            status = stream[position]
            position += 1
            if status == 0xFF:
                kind = stream[position]
                position += 1
                size, position = read_vlq(stream, position)
                payload = stream[position:position + size]
                position += size
                if kind == 0x51:
                    tempos.add(int.from_bytes(payload, "big"))
            elif status in (0xF0, 0xF7):
                size, position = read_vlq(stream, position)
                position += size
            else:
                kind = status & 0xF0
                size = 1 if kind in (0xC0, 0xD0) else 2
                payload = tuple(stream[position:position + size])
                position += size
                if kind in (0x80, 0x90, 0xB0, 0xC0):
                    priority = 0 if kind == 0x80 or kind == 0x90 and payload[1] == 0 else 1
                    events.append((tick, priority, status, payload))
        end_tick = max(end_tick, tick)
    if offset != len(data) or len(tempos) != 1 or end_tick != 32 * TICKS_PER_BAR:
        raise ValueError(f"Unexpected MIDI timeline: {path}")
    return sorted(events), tempos.pop(), ticks_per_beat, end_tick


class OfflineSynth:
    def __init__(self, library, soundfont, sample_rate):
        self.lib = ctypes.CDLL(str(library))
        lib = self.lib
        lib.new_fluid_settings.restype = ctypes.c_void_p
        lib.fluid_settings_setnum.argtypes = [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_double]
        lib.new_fluid_synth.argtypes = [ctypes.c_void_p]
        lib.new_fluid_synth.restype = ctypes.c_void_p
        lib.fluid_synth_sfload.argtypes = [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_int]
        lib.fluid_synth_sfload.restype = ctypes.c_int
        lib.fluid_synth_program_change.argtypes = [ctypes.c_void_p, ctypes.c_int, ctypes.c_int]
        lib.fluid_synth_cc.argtypes = [ctypes.c_void_p, ctypes.c_int, ctypes.c_int, ctypes.c_int]
        lib.fluid_synth_noteon.argtypes = [ctypes.c_void_p, ctypes.c_int, ctypes.c_int, ctypes.c_int]
        lib.fluid_synth_noteoff.argtypes = [ctypes.c_void_p, ctypes.c_int, ctypes.c_int]
        lib.fluid_synth_write_s16.argtypes = [ctypes.c_void_p, ctypes.c_int,
                                             ctypes.c_void_p, ctypes.c_int, ctypes.c_int,
                                             ctypes.c_void_p, ctypes.c_int, ctypes.c_int]
        lib.delete_fluid_synth.argtypes = [ctypes.c_void_p]
        lib.delete_fluid_settings.argtypes = [ctypes.c_void_p]
        self.settings = lib.new_fluid_settings()
        lib.fluid_settings_setnum(self.settings, b"synth.sample-rate", float(sample_rate))
        lib.fluid_settings_setnum(self.settings, b"synth.gain", 0.30)
        self.synth = lib.new_fluid_synth(self.settings)
        if lib.fluid_synth_sfload(self.synth, str(soundfont).encode(), 1) < 0:
            raise RuntimeError(f"Could not load SoundFont: {soundfont}")

    def event(self, status, payload):
        channel = status & 0x0F
        kind = status & 0xF0
        if kind == 0xC0:
            self.lib.fluid_synth_program_change(self.synth, channel, payload[0])
        elif kind == 0xB0:
            self.lib.fluid_synth_cc(self.synth, channel, *payload)
        elif kind == 0x90 and payload[1]:
            self.lib.fluid_synth_noteon(self.synth, channel, *payload)
        else:
            self.lib.fluid_synth_noteoff(self.synth, channel, payload[0])

    def frames(self, count):
        interleaved = (ctypes.c_short * (count * 2))()
        result = self.lib.fluid_synth_write_s16(self.synth, count,
                                                 interleaved, 0, 2,
                                                 interleaved, 1, 2)
        if result != 0:
            raise RuntimeError("FluidSynth failed to render audio")
        return bytes(interleaved)

    def close(self):
        self.lib.delete_fluid_synth(self.synth)
        self.lib.delete_fluid_settings(self.settings)


def render_loop(synth, events, tempo, tpq, end_tick, sample_rate, output):
    cycle_frames = round(end_tick * tempo * sample_rate / (tpq * 1_000_000))
    cursor = 0
    with wave.open(str(output), "wb") as wav:
        wav.setnchannels(2)
        wav.setsampwidth(2)
        wav.setframerate(sample_rate)

        def until(target):
            nonlocal cursor
            while cursor < target:
                count = min(4096, target - cursor)
                # First pass warms up reverb/chorus. Save the second pass so
                # its start and end have the same periodic effect tails.
                count = min(count, cycle_frames - cursor if cursor < cycle_frames
                            else 2 * cycle_frames - cursor)
                pcm = synth.frames(count)
                if cursor >= cycle_frames:
                    wav.writeframesraw(pcm)
                cursor += count

        for repeat in range(2):
            for tick, _, status, payload in events:
                sample = round(tick * tempo * sample_rate / (tpq * 1_000_000))
                until(repeat * cycle_frames + sample)
                synth.event(status, payload)
            until((repeat + 1) * cycle_frames)
    return cycle_frames / sample_rate


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--soundfont", type=Path, required=True)
    parser.add_argument("--fluidsynth-lib", default="libfluidsynth.so.3")
    parser.add_argument("--ffmpeg", default="ffmpeg")
    args = parser.parse_args()
    sample_rate = 44100
    SOUNDS.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="hbk-boss-music-") as temporary:
        for boss in ("stalin", "cj"):
            events, tempo, tpq, end_tick = read_midi(MUSIC / f"{boss}_boss.mid")
            synth = OfflineSynth(args.fluidsynth_lib, args.soundfont, sample_rate)
            try:
                wav = Path(temporary) / f"{boss}.wav"
                duration = render_loop(synth, events, tempo, tpq, end_tick, sample_rate, wav)
            finally:
                synth.close()
            ogg = SOUNDS / f"{boss}_boss.ogg"
            subprocess.run([args.ffmpeg, "-hide_banner", "-loglevel", "error", "-y",
                            "-i", str(wav), "-af", "loudnorm=I=-18:TP=-1.5:LRA=11",
                            "-ar", str(sample_rate), "-c:a", "libvorbis", "-q:a", "5",
                            str(ogg)], check=True)
            print(f"{ogg.relative_to(ROOT)}: {duration:.2f} s")


if __name__ == "__main__":
    main()
