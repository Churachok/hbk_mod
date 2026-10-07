"""Idempotently add room chests to the repository's supplied Grove Street NBT.

No external dependencies. Optionally pass another input NBT with --input.
The imported structure's blocks and block entity data are preserved.
"""
from pathlib import Path
import argparse
import gzip
import struct

OUT = Path(__file__).resolve().parents[1] / 'src/main/resources/data/hbk/structure/grove_street.nbt'
CHESTS = [(88, 1, 26), (66, 1, 78), (9, 2, 60),
          (47, 2, 9), (55, 2, 26), (78, 1, 15), (89, 1, 16),
          (19, 2, 31), (26, 2, 30), (13, 3, 43), (71, 1, 85), (91, 1, 84)]

class Reader:
    def __init__(self, data): self.data, self.offset = data, 0
    def take(self, n):
        out = self.data[self.offset:self.offset+n]
        self.offset += n
        return out
    def num(self, fmt): return struct.unpack('>'+fmt, self.take(struct.calcsize(fmt)))[0]
    def string(self): return self.take(self.num('H')).decode('utf-8')
    def value(self, tag):
        if tag in FORMATS: return self.num(FORMATS[tag])
        if tag == 7: return self.take(self.num('i'))
        if tag == 8: return self.string()
        if tag == 9:
            sub, count = self.num('b'), self.num('i')
            return sub, [self.value(sub) for _ in range(count)]
        if tag == 10:
            result = {}
            while (sub := self.num('b')):
                key = self.string()
                result[key] = (sub, self.value(sub))
            return result
        if tag in (11, 12): return [self.num('i' if tag == 11 else 'q') for _ in range(self.num('i'))]
        raise ValueError(f'Unsupported NBT tag {tag}')

FORMATS = {1:'b', 2:'h', 3:'i', 4:'q', 5:'f', 6:'d'}
def string(s):
    data = s.encode('utf-8')
    return struct.pack('>H', len(data)) + data

def encode(tag, value):
    if tag in FORMATS: return struct.pack('>'+FORMATS[tag], value)
    if tag == 7: return struct.pack('>i',len(value)) + value
    if tag == 8: return string(value)
    if tag == 9:
        sub, entries = value
        return bytes([sub]) + struct.pack('>i',len(entries)) + b''.join(encode(sub,v) for v in entries)
    if tag == 10:
        return b''.join(bytes([t])+string(k)+encode(t,v) for k,(t,v) in value.items())+b'\0'
    if tag in (11,12): return struct.pack('>i',len(value))+b''.join(struct.pack('>i' if tag==11 else '>q',v) for v in value)
    raise ValueError(tag)

def prepare(path):
    reader = Reader(gzip.decompress(path.read_bytes()))
    tag, name = reader.num('b'), reader.string()
    root = reader.value(tag)
    assert root['size'][1][1] == [98, 40, 94], 'Unexpected Grove Street dimensions'
    palette = root['palette'][1][1]
    state = {'Name': (8, 'minecraft:chest'), 'Properties': (10, {
        'facing': (8, 'south'), 'type': (8, 'single'), 'waterlogged': (8, 'false')})}
    if state not in palette: palette.append(state)
    index = palette.index(state)
    blocks = root['blocks'][1][1]
    by_pos = {tuple(b['pos'][1][1]): b for b in blocks}
    # The imported chest is buried beneath a bookshelf: move it into the same room.
    buried = by_pos[(86, 0, 26)]
    if palette[buried['state'][1]]['Name'][1] == 'minecraft:chest':
        buried['state'] = by_pos[(87, 0, 26)]['state']
        buried.pop('nbt', None)
    for pos in CHESTS:
        block = by_pos.get(pos)
        assert block is not None, f'Missing position: {pos}'
        old_name = palette[block['state'][1]]['Name'][1]
        assert old_name in ('minecraft:air', 'minecraft:chest'), f'Would replace {old_name} at {pos}'
        if old_name == 'minecraft:air':
            assert palette[by_pos[(pos[0],pos[1]+1,pos[2])]['state'][1]]['Name'][1] == 'minecraft:air'
            block['state'] = (3, index)
        block['nbt'] = (10, {'id': (8, 'minecraft:chest'),
                             'LootTable': (8, 'hbk:chests/grove_street')})
    OUT.parent.mkdir(parents=True, exist_ok=True)
    # Fixed gzip timestamp makes repeated preparation byte-for-byte reproducible.
    OUT.write_bytes(gzip.compress(bytes([tag])+string(name)+encode(tag, root), mtime=0))
    print(f'Prepared {OUT.name}: 98x40x94, {len(CHESTS)} room chests')

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--input', type=Path, default=OUT)
    prepare(parser.parse_args().input)
