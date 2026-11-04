#!/usr/bin/env python3
"""Export the two original v2 atlases to pixel sprites, machine models and armour UVs.

Only repository-local sources are consumed. Pillow; no Minecraft installation or network.
"""
from pathlib import Path
import json
import math
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / 'art/universe_alloys'
A = ROOT / 'src/main/resources/assets/hbk'

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')

def save(path, image):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)

def cell(atlas, index, opaque=False):
    x, y = index % 6, index // 6
    w, h = atlas.size
    image = atlas.crop((round(x*w/6), round(y*h/6), round((x+1)*w/6), round((y+1)*h/6)))
    image = image.resize((16, 16), Image.Resampling.NEAREST)
    alpha = image.getchannel('A').point(lambda a: 255 if opaque or a >= 160 else 0)
    # A small finite palette removes sub-pixel source noise without smoothing any edge.
    rgb = image.convert('RGB').quantize(colors=16, method=Image.Quantize.MEDIANCUT).convert('RGBA')
    rgb.putalpha(alpha)
    if not opaque:
        # Remove isolated one-pixel atlas speckles while retaining diagonally joined tool handles.
        remaining = {(x,y) for x in range(16) for y in range(16) if rgb.getpixel((x,y))[3]}
        while remaining:
            seed = remaining.pop(); component = {seed}; queue = [seed]
            while queue:
                x,y = queue.pop()
                for dx in [-1,0,1]:
                    for dy in [-1,0,1]:
                        nxt = (x+dx,y+dy)
                        if nxt in remaining: remaining.remove(nxt); component.add(nxt); queue.append(nxt)
            if len(component) < 3:
                for pos in component: rgb.putpixel(pos,(0,0,0,0))
    return rgb

def model(name, handheld=False):
    return {'parent': 'minecraft:item/' + ('handheld' if handheld else 'generated'),
            'textures': {'layer0': 'hbk:item/alloys/' + name}}

def item_definition(name):
    return {'type': 'minecraft:model', 'model': 'hbk:item/' + name}

def box(lo, hi, faces):
    return {'from': lo, 'to': hi, 'faces': {face: {'texture': '#' + faces.get(face, 'side'), 'uv': [0, 0, 16, 16]} for face in ['up', 'down', 'north', 'south', 'west', 'east']}}

def armour(sprite, role, name):
    """Map authored motifs into the actual vanilla 64x32 armour UV islands."""
    image = Image.new('RGBA', (64, 32))
    draw = ImageDraw.Draw(image)
    colors = [rgb for count, rgb in sprite.getcolors(256) if rgb[3] and min(rgb[:3]) > 35]
    base = sorted(colors, key=lambda c: sum(c[:3]))[len(colors)//2]
    dark = tuple(max(12, int(c*.50)) for c in base[:3]) + (255,)
    light = tuple(min(255, int(c*1.30)) for c in base[:3]) + (255,)
    def plate(rect):
        draw.rectangle(rect, fill=base, outline=dark)
        draw.line((rect[0]+1, rect[1]+1, rect[2]-1, rect[1]+1), fill=light)
        if rect[3]-rect[1] > 5:
            draw.line((rect[0]+1, rect[3]-2, rect[2]-1, rect[3]-2), fill=dark)
    if role == 'helmet':
        for rect in [(8,0,15,7), (16,0,23,7), (0,8,7,15), (8,8,15,15), (16,8,23,15), (24,8,31,15)]: plate(rect)
        if name == 'hermetic_helmet':
            draw.rectangle((9,11,14,14), fill=(40,74,85,255)); draw.line((10,11,13,11), fill=(173,222,225,255))
            for x in [1,17]:
                draw.rectangle((x,12,x+4,14), fill=dark); draw.line((x+1,13,x+3,13), fill=light)
        else:
            draw.rectangle((11,9,13,11), fill=(248,189,67,255)); draw.point((12,9), fill=(255,246,174,255))
            draw.rectangle((9,13,14,15), fill=(0,0,0,0))
    elif role == 'chestplate':
        for rect in [(20,16,27,19),(28,16,35,19),(16,20,19,31),(20,20,27,31),(28,20,31,31),(32,20,39,31),
                     (44,16,47,19),(48,16,51,19),(40,20,43,31),(44,20,47,31),(48,20,51,31),(52,20,55,31)]: plate(rect)
        motif = sprite.crop((4,4,12,12)).resize((6,6), Image.Resampling.NEAREST)
        image.alpha_composite(motif, (21,22))
        for x in [33,37]: draw.line((x,21,x,29), fill=light, width=1)
        if name == 'gliding_chestplate':
            for y in range(22,29,2): draw.line((33,y,38,y+1), fill=(220,210,172,255))
        else:
            draw.ellipse((33,23,38,28), fill=dark, outline=(159,126,194,255))
    else:
        for rect in [(4,16,7,19),(8,16,11,19),(0,20,3,31),(4,20,7,31),(8,20,11,31),(12,20,15,31)]: plate(rect)
        # Only the boot portion of each leg is armoured; cuff, valve/fins and reinforced soles differ.
        draw.rectangle((0,20,15,25), fill=(0,0,0,0))
        for x in [0,4,8,12]:
            draw.line((x,26,x+3,26), fill=light); draw.line((x,31,x+3,31), fill=dark)
        motif = sprite.crop((2,9,8,14)).resize((4,4), Image.Resampling.NEAREST)
        image.alpha_composite(motif, (4,27))
    return image

def export(alloys):
    rewards = Image.open(ART / 'rewards-v2.png').convert('RGBA')
    devices = Image.open(ART / 'devices-v2.png').convert('RGBA')
    preview = Image.new('RGBA', (9*22, 6*24), (38,40,43,255))
    portable = [r for a in alloys for r in a['rewards'] if r['role'] != 'device']
    device_list = [r for a in alloys for r in a['rewards'] if r['role'] == 'device']
    icons = {}
    for i, reward in enumerate(portable):
        name, role = reward['id'], reward['role']
        sprite = cell(rewards, i)
        icons[name] = sprite
        save(A / f'textures/item/alloys/{name}.png', sprite)
        asset = model(name, role in ['pickaxe','axe','sword','hoe','shovel'])
        definition = item_definition(name)
        if role == 'shield':
            # Custom generated geometry retains a distinct silhouette and normal blocking poses.
            asset['display'] = {'firstperson_righthand': {'rotation':[0,-75,10], 'translation':[1,1,-2], 'scale':[1.6]*3},
                'firstperson_lefthand': {'rotation':[0,75,-10], 'translation':[1,1,-2], 'scale':[1.6]*3},
                'thirdperson_righthand': {'rotation':[0,90,0], 'translation':[0,2,0], 'scale':[1.3]*3}}
            blocking = json.loads(json.dumps(asset))
            blocking['display']['firstperson_righthand'] = {'rotation':[0,-25,-5], 'translation':[-1,2,-1], 'scale':[1.7]*3}
            blocking['display']['firstperson_lefthand'] = {'rotation':[0,25,5], 'translation':[-1,2,-1], 'scale':[1.7]*3}
            write(A / f'models/item/{name}_blocking.json', blocking)
            definition = {'type':'minecraft:condition','property':'minecraft:using_item', 'on_true':item_definition(name+'_blocking'), 'on_false':item_definition(name)}
        elif role == 'crossbow':
            asset['display'] = {'thirdperson_righthand': {'rotation':[-90,0,-60], 'translation':[2,.1,-3], 'scale':[.9]*3},
                'thirdperson_lefthand': {'rotation':[-90,0,30], 'translation':[2,.1,-3], 'scale':[.9]*3},
                'firstperson_righthand': {'rotation':[-90,0,-55], 'translation':[1.13,3.2,1.13], 'scale':[.68]*3},
                'firstperson_lefthand': {'rotation':[-90,0,35], 'translation':[1.13,3.2,1.13], 'scale':[.68]*3}}
            for state in ['pulling_0','pulling_1','pulling_2','arrow','firework']:
                variant = sprite.copy(); d = ImageDraw.Draw(variant)
                offset = 1 if state=='pulling_0' else 2 if state=='pulling_1' else 3
                d.line((5,9,9+offset,5), fill=(133,224,230,255), width=1)
                if state in ['arrow','firework']: d.line((5,10,12,3), fill=(240,190,86,255), width=1)
                save(A / f'textures/item/alloys/{name}_{state}.png', variant)
                state_model = json.loads(json.dumps(asset)); state_model['textures']['layer0'] += '_' + state
                write(A / f'models/item/{name}_{state}.json', state_model)
            definition = {'type':'minecraft:select', 'property':'minecraft:charge_type','cases':[
                {'when':charge, 'model':item_definition(name+'_'+state)} for charge,state in [('arrow','arrow'),('rocket','firework')]],
                'fallback': {'type':'minecraft:condition','property':'minecraft:using_item','on_false':item_definition(name),
                    'on_true':{'type':'minecraft:range_dispatch','property':'minecraft:crossbow/pull','fallback':item_definition(name+'_pulling_0'),
                        'entries':[{'threshold':.58,'model':item_definition(name+'_pulling_1')},{'threshold':1.,'model':item_definition(name+'_pulling_2')}]}}}
        elif name in ['entry_compass','ore_compass']:
            frames = []
            for frame in range(32):
                dial = sprite.copy(); d = ImageDraw.Draw(dial)
                d.ellipse((4,5,11,12), fill=(39,51,51,255))
                angle = frame * math.pi/16
                end = (round(7.5+3*math.sin(angle)),round(8.5-3*math.cos(angle)))
                d.line((8,9,*end), fill=(192,235,220,255), width=1)
                d.point((8,9), fill=(245,199,92,255))
                state = name+f'_{frame:02}'
                save(A / f'textures/item/alloys/{state}.png', dial)
                write(A / f'models/item/{state}.json', model(state))
                frames.append(item_definition(state))
            definition = {'type':'minecraft:range_dispatch','property':'minecraft:compass','target':'lodestone','scale':32.,
                'fallback':frames[0], 'entries':[{'threshold':i/1.,'model':frames[i]} for i in range(32)]}
        write(A / f'models/item/{name}.json', asset)
        write(A / f'items/{name}.json', {'model':definition})
        preview.alpha_composite(sprite, ((i%9)*22+3,(i//9)*24+4))
    for index, reward in enumerate(device_list):
        name = reward['id']
        for n, face in enumerate(['front','side','top']):
            tex = cell(devices, index*3+n, opaque=name!='phase_glass')
            save(A / f'textures/block/alloys/{name}_{face}.png', tex)
        textures = {'particle':'hbk:block/alloys/'+name+'_front', **{f:'hbk:block/alloys/'+name+'_'+f for f in ['front','side','top']}}
        faces = {'north':'front','south':'front','up':'top','down':'side'}
        elements = [box([0,0,0],[16,16,16],faces)]
        if name in ['lift_platform','transition_plate']:
            elements = [box([0,0,0],[16,2,16],faces)]
        elif name=='recycling_anvil':
            elements = [box([1,0,2],[15,3,14],faces),box([4,3,4],[12,10,12],faces),box([0,10,1],[16,16,15],faces)]
        elif name=='wind_vane':
            elements=[box([2,0,2],[14,3,14],faces),box([7,3,7],[9,12,9],faces),box([1,10,6],[15,14,10],faces)]
        elif name=='washing_trough':
            elements=[box([0,0,0],[16,4,16],faces),box([0,4,0],[2,12,16],faces),box([14,4,0],[16,12,16],faces),box([2,4,0],[14,12,2],faces),box([2,4,14],[14,12,16],faces)]
        block_model = {'parent':'minecraft:block/block','textures':textures,'elements':elements}
        write(A / f'models/block/{name}.json', block_model)
        state = {'variants':{'':{'model':'hbk:block/'+name}}}
        if name=='phase_glass':
            active = json.loads(json.dumps(block_model))
            for face in ['front','side','top']: active['textures'][face] += '_active'
            for face in ['front','side','top']:
                img = Image.open(A / f'textures/block/alloys/{name}_{face}.png').convert('RGBA')
                img.putalpha(img.getchannel('A').point(lambda a: a//2))
                save(A / f'textures/block/alloys/{name}_{face}_active.png', img)
            write(A / f'models/block/{name}_active.json', active)
            state={'variants':{'powered=false':{'model':'hbk:block/'+name},'powered=true':{'model':'hbk:block/'+name+'_active'}}}
        write(A / f'blockstates/{name}.json',state)
        write(A / f'items/{name}.json',{'model':{'type':'minecraft:model','model':'hbk:block/'+name}})
        preview.alpha_composite(cell(devices,index*3), (((35+index)%9)*22+3,((35+index)//9)*24+4))
    for alloy in alloys:
        for reward in alloy['rewards']:
            if reward['role'] in ['helmet','chestplate','boots']:
                uv = armour(icons[reward['id']], reward['role'], reward['id'])
                for category in ['humanoid','humanoid_baby','humanoid_leggings']:
                    save(A / f'textures/entity/equipment/{category}/alloys/{alloy["id"]}.png',uv)
                write(A / f'equipment/{alloy["id"]}.json', {'layers': {category:[{'texture':'hbk:alloys/'+alloy['id']}]
                    for category in ['humanoid','humanoid_baby','humanoid_leggings']}})
    # The cart receives its own UV sheet and two original timber cargo cases.
    metal = Image.open(A / 'textures/block/alloys/lift_platform_side.png').convert('RGBA')
    cart = Image.new('RGBA',(64,32))
    for x in range(0,64,16):
        for y in range(0,32,16): cart.alpha_composite(metal,(x,y))
    ImageDraw.Draw(cart).rectangle((2,3,61,4),fill=(172,133,67,255))
    save(A / 'textures/entity/cargo_minecart.png',cart)
    crate = Image.new('RGBA',(16,16),(106,77,49,255)); d = ImageDraw.Draw(crate)
    for y in [1,5,9,13]: d.line((0,y,15,y),fill=(62,51,39,255))
    for x in [1,13]: d.rectangle((x,0,x+1,15),fill=(135,152,159,255))
    d.rectangle((6,6,9,10),fill=(63,69,72,255)); d.point((7,7),fill=(224,184,97,255))
    save(A / 'textures/block/alloys/cargo_crate.png',crate)
    write(A / 'models/block/cargo_crate.json',{'parent':'minecraft:block/block','textures':{'side':'hbk:block/alloys/cargo_crate','particle':'hbk:block/alloys/cargo_crate'},
        'elements':[box([0,0,1],[7,13,15],{}),box([9,0,1],[16,13,15],{})]})
    write(A / 'blockstates/cargo_crate.json',{'variants':{'':{'model':'hbk:block/cargo_crate'}}})
    # A custom rope ladder face rather than an unchanged Minecraft ladder reference.
    ladder = Image.new('RGBA',(16,16)); d=ImageDraw.Draw(ladder)
    for x in [2,12]: d.rectangle((x,0,x+1,15), fill=(129,153,151,255))
    for y in [2,7,12]: d.rectangle((3,y,12,y+1), fill=(170,137,89,255))
    save(A / 'textures/block/alloys/rope_ladder.png',ladder)
    write(A / 'models/block/alloy_rope_ladder.json',{'parent':'minecraft:block/ladder','textures':{'texture':'hbk:block/alloys/rope_ladder'}})
    write(A / 'blockstates/alloy_rope_ladder.json',{'variants':{'facing='+f:{'model':'hbk:block/alloy_rope_ladder','y':y} for f,y in [('north',0),('south',180),('west',270),('east',90)]}})
    save(ART / 'rewards-v2-preview.png',preview.resize((1188,864),Image.Resampling.NEAREST))

if __name__=='__main__':
    export(json.loads((ROOT / 'src/main/resources/hbk/alloys.json').read_text()))
