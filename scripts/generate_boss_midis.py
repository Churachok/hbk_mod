"""Generate the two original, loopable boss battle MIDI sketches.

No third-party packages are needed. MIDI is source material for arranging and
rendering; Minecraft cannot play .mid files directly.
"""

from pathlib import Path
import struct


TICKS = 480
BAR = 4 * TICKS
OUT = Path(__file__).resolve().parents[1] / "music"


def variable_length(value):
    data = [value & 0x7F]
    value >>= 7
    while value:
        data.insert(0, 0x80 | (value & 0x7F))
        value >>= 7
    return bytes(data)


def at(bar, beat=0):
    return round((bar * 4 + beat) * TICKS)


class Track:
    def __init__(self, name, channel=None, program=None, pan=64, volume=100):
        self.events = []
        self.channel = channel
        self.meta(0, 0x03, name.encode("utf-8"))
        if channel is not None:
            if program is not None:
                self.raw(0, bytes((0xC0 | channel, program)))
            self.raw(0, bytes((0xB0 | channel, 7, volume)))
            self.raw(0, bytes((0xB0 | channel, 10, pan)))

    def raw(self, tick, message, order=1):
        self.events.append((tick, order, message))

    def meta(self, tick, kind, payload):
        self.raw(tick, b"\xff" + bytes((kind,)) + variable_length(len(payload)) + payload, 0)

    def note(self, start, duration, pitch, velocity=85):
        assert self.channel is not None and 0 <= pitch <= 127 and duration > 0
        begin = round(start * TICKS)
        end = round((start + duration) * TICKS)
        self.raw(begin, bytes((0x90 | self.channel, pitch, velocity)), 2)
        self.raw(end, bytes((0x80 | self.channel, pitch, 0)), 0)

    def chord(self, start, duration, pitches, velocity=75):
        for pitch in dict.fromkeys(pitches):
            self.note(start, duration, pitch, velocity)

    def encode(self, end_tick):
        output = bytearray()
        previous = 0
        for tick, _, message in sorted(self.events, key=lambda e: (e[0], e[1])):
            output.extend(variable_length(tick - previous))
            output.extend(message)
            previous = tick
        output.extend(variable_length(end_tick - previous))
        output.extend(b"\xff\x2f\x00")
        return b"MTrk" + struct.pack(">I", len(output)) + output


def conductor(title, bpm, key, markers):
    track = Track(title)
    track.meta(0, 0x51, round(60_000_000 / bpm).to_bytes(3, "big"))
    track.meta(0, 0x58, bytes((4, 2, 24, 8)))
    track.meta(0, 0x59, bytes((key & 0xFF, 1)))
    for bar, label in markers:
        track.meta(at(bar), 0x06, label.encode("utf-8"))
    return track


def write_midi(filename, tracks, bars=32):
    end_tick = bars * BAR
    header = b"MThd" + struct.pack(">IHHH", 6, 1, len(tracks), TICKS)
    OUT.mkdir(exist_ok=True)
    (OUT / filename).write_bytes(header + b"".join(t.encode(end_tick) for t in tracks))


def stalin():
    """D minor funeral march: artillery, NKVD call, silhouette rage."""
    tracks = [conductor("STALIN | Iron Procession", 112, -1, [
        (0, "Dacha: ominous approach"), (4, "Rocket march"),
        (12, "NKVD: health threshold"), (24, "Western chestplate: rage"),
        (28, "Final barrage / loop return"),
    ])]
    strings = Track("Low strings - rocket motor", 0, 48, 42, 99)
    bass = Track("Contrabass and cello - giant footsteps", 1, 42, 30, 105)
    brass = Track("Trombones and horns - original Soviet-style fanfare", 2, 57, 75, 108)
    choir = Track("Gothic male choir", 3, 52, 58, 92)
    organ = Track("Cathedral organ", 4, 19, 64, 73)
    bayan = Track("Bayan / accordion color", 5, 21, 80, 69)
    bells = Track("Tolling bells", 6, 14, 62, 77)
    drums = Track("March snare and explosions (GM drums)", 9, pan=64, volume=104)
    timpani = Track("Timpani", 7, 47, 48, 101)
    squad_brass = Track("NKVD five-note brass calls", 8, 61, 65, 108)
    tracks.extend((strings, bass, brass, choir, organ, bayan, bells, drums, timpani, squad_brass))

    # Original minor-mode progression; no anthem melody is quoted.
    roots = [38, 34, 31, 33, 38, 36, 34, 33] * 4  # D2 Bb1 G1 A1
    chords = [
        (50, 53, 57), (46, 50, 53), (43, 46, 50), (45, 49, 52),
        (50, 53, 57), (48, 52, 55), (46, 50, 53), (45, 49, 52),
    ] * 4
    fanfare = [
        [(0, 62, 1.4), (1.5, 65, .45), (2, 69, .9), (3, 65, .8)],
        [(0, 70, 1.4), (1.5, 69, .45), (2, 65, .9), (3, 62, .8)],
        [(0, 67, 1), (1, 70, .45), (1.5, 74, .45), (2, 70, .9), (3, 67, .8)],
        [(0, 69, .8), (1, 73, .8), (2, 76, 1.7)],
        [(0, 74, 1.5), (1.5, 72, .45), (2, 69, .9), (3, 65, .8)],
        [(0, 67, .9), (1, 64, .9), (2, 67, .9), (3, 72, .8)],
        [(0, 70, 1), (1, 74, .9), (2, 70, .9), (3, 65, .8)],
        [(0, 69, 1), (1, 73, .9), (2, 76, .65), (3, 73, .8)],
    ]
    for bar in range(32):
        root = roots[bar]
        chord = chords[bar]
        intensity = 0 if bar < 4 else 2 if 24 <= bar else 1
        bass.note(bar * 4, 3.75, root, 73 + intensity * 8)
        organ.chord(bar * 4, 3.9, (root + 12, chord[0], chord[1]), 42 + intensity * 6)
        choir.chord(bar * 4, 3.8, tuple(p + 12 for p in chord), 57 + intensity * 6)
        if bar >= 4:
            # Regiment-like low-string eighths accelerate during the rage.
            for step in range(8):
                p = root + (12 if step % 2 == 0 else 19)
                strings.note(bar * 4 + step * .5, .42, p, 58 + intensity * 8 + (step % 4 == 0) * 7)
            for beat, pitch, duration in fanfare[bar % 8]:
                brass.note(bar * 4 + beat, duration, pitch, 82 + intensity * 6)
            if bar % 8 in (0, 1, 4, 5):
                bayan.chord(bar * 4 + 1.0, .7, (chord[0] + 12, chord[1] + 12), 62)
                bayan.chord(bar * 4 + 3.0, .7, (chord[0] + 12, chord[2] + 12), 65)
        else:
            strings.note(bar * 4 + 2, 1.7, root + 12, 55)
            if bar in (0, 2):
                bells.note(bar * 4, 2.6, 74 if bar == 0 else 69, 75)

        drums.note(bar * 4, .11, 36, 96 if bar >= 4 else 68)
        drums.note(bar * 4 + 2, .11, 36, 90 if bar >= 4 else 62)
        if bar >= 4:
            for beat in (1, 3):
                drums.note(bar * 4 + beat, .1, 38, 88 + intensity * 5)
            for beat in (0, .5, 1.5, 2, 2.5, 3.5):
                drums.note(bar * 4 + beat, .08, 42, 50 + intensity * 6)
        if bar in (4, 12, 16, 24, 28):
            drums.note(bar * 4, .35, 49, 98)
            timpani.note(bar * 4, .8, root + 12, 100)

    # Every lost tenth of health calls a five-person squad: five hard stabs.
    for bar in (12, 20):
        for i in range(5):
            offset = i * .5
            squad_brass.chord(bar * 4 + offset, .34, (62, 65, 69), 100 - i * 3)
            drums.note(bar * 4 + offset, .12, 40, 104 - i * 4)
        bells.note(bar * 4, 1.7, 74, 106)
    # Horn call for the Western chestplate's rage and extra NKVD squad.
    squad_brass.chord(24 * 4, 1.7, (62, 69, 74), 112)
    bells.note(24 * 4, 2.5, 86, 108)
    for bar in range(24, 28):
        for beat in (0, 1, 2, 3):
            if not (bar == 24 and beat == 0):
                timpani.note(bar * 4 + beat, .3, roots[bar] + 12, 88 + 6 * (beat == 0))
    # Massed, ominous end implies the death blast without a hard terminal stop.
    for beat in (0, .5, 1, 1.5, 2, 2.5, 3):
        drums.note(31 * 4 + beat, .12, 40, 83 + round(beat * 5))
    write_midi("stalin_boss.mid", tracks)


def cj():
    """G minor battle score; hip-hop timbres support the orchestral attack."""
    tracks = [conductor("CJ | Ten-Rocket Requiem", 120, -2, [
        (0, "Full orchestra: boss enters"), (8, "Rocket pursuit"),
        (16, "Ten-rocket volley"), (24, "Choir and brass escalation"),
        (28, "Final ten-rocket volley / loop return"),
    ])]
    strings = Track("Relentless sixteenth-note strings", 0, 48, 36, 105)
    low_strings = Track("Cellos - giant pursuit", 1, 42, 27, 101)
    brass = Track("Brass section - battle theme", 2, 61, 72, 112)
    horns = Track("French horns - answering calls", 3, 60, 43, 100)
    choir = Track("Gothic cathedral choir", 4, 52, 62, 102)
    choir_stabs = Track("Choir battle accents", 5, 52, 75, 95)
    organ = Track("Cathedral pedal organ", 6, 19, 58, 65)
    bass = Track("Syncopated 2000s-style synth bass", 7, 38, 51, 79)
    rhodes = Track("Dark electric piano color", 8, 4, 83, 55)
    drums = Track("Cinematic drums with swung breakbeat (GM)", 9, pan=64, volume=110)
    timpani = Track("Battle timpani", 10, 47, 48, 108)
    hits = Track("Ten-rocket orchestral hits", 11, 55, 58, 108)
    volley_brass = Track("Ten-rocket rising brass", 12, 61, 77, 108)
    volley_choir = Track("Ten-rocket choir response", 13, 52, 69, 101)
    tracks.extend((strings, low_strings, brass, horns, choir, choir_stabs,
                   organ, bass, rhodes, drums, timpani, hits, volley_brass, volley_choir))

    # Minor harmony and an original brass motif. The beat never leads the theme.
    roots = [43, 39, 36, 38, 43, 41, 39, 38] * 4
    chords = [
        (55, 58, 62, 65), (51, 55, 58, 62), (48, 51, 55, 62), (50, 54, 57, 63),
        (55, 58, 62, 65), (53, 57, 60, 64), (51, 55, 58, 62), (50, 54, 57, 63),
    ] * 4
    theme = [
        [(0, 67, .43), (.5, 70, .42), (1, 74, .82), (2, 79, 1.65)],
        [(0, 75, .9), (1, 74, .42), (1.5, 70, .42), (2, 67, .8), (3, 70, .8)],
        [(0, 72, .8), (1, 75, .8), (2, 79, .9), (3, 75, .8)],
        [(0, 74, .4), (.5, 78, .4), (1, 81, .9), (2, 78, .9), (3, 74, .8)],
        [(0, 79, .9), (1, 82, .9), (2, 86, .85), (3, 82, .8)],
        [(0, 77, .78), (1, 72, .42), (1.5, 69, .42), (2, 72, .85), (3, 77, .8)],
        [(0, 75, .75), (1, 79, .42), (1.5, 82, .4), (2, 79, .85), (3, 75, .8)],
        [(0, 74, .4), (.5, 78, .4), (1, 81, .85), (2, 78, .85), (3, 74, .8)],
    ]
    for bar in range(32):
        root = roots[bar]
        chord = chords[bar]
        fierce = bar >= 16
        choir.chord(bar * 4, 3.82, tuple(p + 12 for p in chord[:3]), 76 + fierce * 8)
        organ.chord(bar * 4, 3.9, (root, root + 12), 40 + fierce * 6)
        for beat in (0, 2):
            low_strings.note(bar * 4 + beat, 1.75, root + 12, 84 + fierce * 8)
            horns.chord(bar * 4 + beat, 1.45, (chord[0] + 12, chord[2] + 12), 73 + fierce * 7)
        for step in range(16):
            pitch = root + 24 if step % 4 in (0, 3) else chord[1] + 12
            strings.note(bar * 4 + step * .25, .19, pitch,
                         61 + (step % 4 == 0) * 9 + fierce * 9)
        for beat, pitch, duration in theme[bar % 8]:
            brass.note(bar * 4 + beat, duration, pitch, 90 + fierce * 9)
        choir_stabs.chord(bar * 4 + 1.5, .42,
                          (chord[0] + 12, chord[2] + 12), 67 + fierce * 7)
        choir_stabs.chord(bar * 4 + 3.5, .37,
                          (chord[0] + 12, chord[2] + 12), 69 + fierce * 7)

        # Hiphop DNA: offbeat synth bass, dusty keyboard and swung hats, all below the orchestra.
        for beat, duration in ((0, .67), (1.5, .33), (2, .6), (3.5, .32)):
            bass.note(bar * 4 + beat, duration,
                      root if beat != 3.5 else roots[(bar + 1) % 32] - 1,
                      76 if beat in (0, 2) else 62)
        if bar >= 8:
            rhodes.chord(bar * 4 + .75, .43, chord, 48)
            rhodes.chord(bar * 4 + 2.75, .43, chord, 46)
        for beat, pitch, velocity in ((0, 36, 105), (1.5, 36, 74),
                                      (2, 36, 93), (3.5, 36, 69),
                                      (2, 38, 92), (2, 39, 56),
                                      (1, 45, 76), (3, 41, 79)):
            drums.note(bar * 4 + beat, .1, pitch, velocity + fierce * 5)
        drums.note(bar * 4 + 1.83, .07, 38, 34)  # ghost note
        for beat in (0, .67, 1, 1.67, 2, 2.67, 3, 3.67):
            drums.note(bar * 4 + beat, .06, 42, 34 + (beat == 0) * 8)
        timpani.note(bar * 4, .62, root + 12, 88 + fierce * 10)
        timpani.note(bar * 4 + 3, .44, root + 12, 76 + fierce * 8)
        if bar in (0, 8, 16, 24, 28):
            drums.note(bar * 4, .32, 49, 100)
            hits.note(bar * 4, .55, root + 24, 102)

    # The exact count of ten short rising strikes mirrors CJ's special volley.
    for bar in (16, 28):
        for i in range(10):
            onset = bar * 4 + .25 + i * .2
            hits.note(onset, .12, 55 + i, 80 + i * 4)
            volley_brass.note(onset, .12, 67 + i, 80 + i * 4)
            drums.note(onset, .07, 47 if i % 2 else 48, 78 + i * 4)
        volley_choir.chord(bar * 4 + 2.5, 1.25, (67, 70, 74), 98)
    write_midi("cj_boss.mid", tracks)


if __name__ == "__main__":
    stalin()
    cj()
    for path in (OUT / "stalin_boss.mid", OUT / "cj_boss.mid"):
        print(f"{path.relative_to(OUT.parent)}: {path.stat().st_size} bytes")
