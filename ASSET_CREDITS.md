# Asset credits

`src/main/resources/assets/hbk/sounds/canned_laughter.ogg` uses the first five seconds (starting at 0.2 s) of **Sitcom Laughter 9x, Small Audience**, by **Kinoton**:
https://freesound.org/people/Kinoton/sounds/383207/

License: **CC0 1.0**, https://creativecommons.org/publicdomain/zero/1.0/ .
The public HQ preview was converted to mono Ogg Vorbis (44.1 kHz), with short fades. No speech or music was added.

Inventory assets:

- `denis_doshirak.png`: built-in imagegen edit of the earlier packaging artwork, restored detailed photographic-style packaging with a transparent background. Smooth Lanczos downsampling to 240×240, centered on a transparent 256×256 canvas; no deliberate pixelation. The effect icon uses a 64×64 copy.
- `doshirak_kettle.png` and `hard_doshirak_kettle.png`: generated from the existing code-native kettle drawing in `scripts/generate_kitchen_update_assets.py`, with only its enamel palette changed to red. Both 32×32 silhouettes, dark outlines, tea windows and cooked steam exactly match their respective original kettle sprites. Rebuild with `python3 scripts/generate_kitchen_update_assets.py --noodle-kettles-only` (Pillow).
- `funny_button.png`: black box with a raised yellow button, three-quarter view, 16×16 Minecraft pixel-art sprite, transparent alpha.
- `goshas_dandruff.png`: several separate white grains with gray shading, 16×16 Minecraft pixel-art sprite, transparent alpha.

Generated source images and the external packaging reference are not runtime dependencies. The two noodle kettles use separate raw/cooked textures; their original gray-blue counterparts are unchanged.

## Liberal blood assets

`liberal_blood_bucket.png` is the Minecraft 26.2 `water_bucket.png` with only its five blue water colors replaced by scarlet/red shades. All metal, outline and alpha pixels are identical to vanilla. `young_liberal.png` is an 18×18 icon with three equal white-blue-white stripes on a transparent background. These assets use deterministic palette/shape generation, not imagegen. Rebuild with `python3 scripts/generate_liberal_blood_assets.py`; optionally pass a Minecraft client archive with `--minecraft-jar`.

Night sky flowers reference vanilla `minecraft:textures/block/poppy.png`, `dandelion.png`, `oxeye_daisy.png` and `blue_orchid.png` at runtime. They are not copied or redrawn, and resource packs can replace them. The Minecraft archive is only a source for the bucket recolor and is not shipped with the mod.

## Generation prompts (built-in imagegen)

### Doshirak, detail-restoration edit of the earlier packaging artwork

Use case: precise-object-edit. Edit target: supplied transparent Doshirak package inventory artwork. Replace the coarse chunky pixel rendering with a crisp detailed photographic-looking cutout of the same real package. Preserve this package's white rectangular plastic tray, red label, large yellow Cyrillic Доширак brand lettering with black outline, green badge, bottom food photograph of noodles, vegetables and beef, barcode and diagonal tilted perspective. Restore smooth fine image detail and clear lettering, not blocky pixels, not low-resolution pixel art. Maintain the overall composition and recognizable retail packaging, one single object filling a square canvas with a small transparent margin. Remove every background and cast shadow; actual transparent alpha outside the white tray, white tray stays opaque. No extra objects, no added text outside the label. Intended for a high-detail 256x256 Minecraft inventory sprite, no deliberate pixelation.

### Funny button

Use case: stylized-concept. Minecraft inventory item sprite, one funny button: small black cubic plastic box seen from above in three-quarter perspective, one large raised bright yellow round button on top. Crisp chunky pixel art matching vanilla Minecraft item textures, 16x16 logical pixel grid, no text, no outlines outside object, no cast shadow, actual transparent background. Box and button fill most canvas, single object centered.

### Dandruff

Use case: stylized-concept. Single Minecraft inventory item sprite representing dandruff: a loose small cluster of about eight white irregular tiny flakes/grains with light grey pixel shadows. White grains separated by transparent gaps, no skin or hair, no bag/container, no text or shadow. Crisp chunky vanilla Minecraft pixel art on a logical 16x16 grid, centered occupying 12x12 pixels. Actual transparent background.

## Печь времени

- `assets/hbk/models/block/time_furnace_*.json`, `models/item/time_furnace.json` и пять текстур `assets/hbk/textures/block/time_furnace_*.png` — оригинальные кубоидные модели и пиксельные материалы по предоставленному владельцем концепту печи. Воспроизводятся командой `python3 scripts/generate_time_furnace_assets.py` без внешних библиотек или изображений.

## Аномальные зоны

- `assets/hbk/textures/block/fractured_block.png`, `fractured_block_phantom.png` и `assets/hbk/textures/item/spatial_shard.png` — новые ассеты, созданные встроенным imagegen по мотивам предоставленного владельцем концепта «Разломанный блок». Итоговые текстуры уменьшены до 32×32 ближайшим соседом; призрачная версия использует тот же рисунок с альфой 32%. Полные промпты приведены ниже. Концепт и исходные изображения генерации для сборки и запуска не требуются.

- `assets/hbk/textures/environment/anomaly_641.png` — предоставленное владельцем изображение `641.png`, включено без изменений; исходный внешний файл не требуется при сборке или запуске. `example.png` используется только как концепт рельефа.
- `assets/hbk/sounds/anomaly_glitch.ogg` — оригинальный синтезированный сигнал цифровых помех (0,65 секунды), без внешних записей; воспроизводится `python3 scripts/generate_anomaly_glitch_sound.py` (FFmpeg).
- `assets/hbk/sounds/ambient/anomaly_ambient.ogg` — предоставленная владельцем петля из `anomaly.zip`, 120 секунд, моно, 48 кГц; файл переименован без изменения аудиоданных. Согласно описанию архива, содержит окружение Minecraft и обработанный слой «26. Black Fairy.mp3» из участка 23,0–30,9 секунды. Архив и внешние исходники для сборки или запуска не требуются.

### Fractured block — built-in imagegen prompt

Use case: stylized-concept. Asset type: Minecraft pixel-art seamless square block face texture, a single flat orthographic texture filling the entire canvas edge to edge, no cube or perspective. Primary request: dark shattered slate/gray cobblestone with jagged electric-cyan luminous fissures, matching the user's broken spatial block concept: small charcoal, slate-gray, blue-gray stone fragments framing one branching bright cyan rift. Style: deliberately chunky clean Minecraft pixel art on a 32 by 32 logical pixel grid enlarged by nearest-neighbor, no tiny detail or soft painting. Palette: charcoal #262d35, blue-gray #455c71, stone-gray #788896, electric blue #158ad8, cyan #60e4ff, very light blue #cef8ff. Broad angular bright zigzag cyan fault passes vertically with a small branch to the right, chipped stones and pixel highlights around it. Even flat lighting except glow contained inside cyan pixels, no bloom outside surface. Seamless tileable edges. No text, no labels, no mockup, no border, no shadow background, no transparent background. This is a new game texture, not a concept sheet.

### Spatial shard — built-in imagegen prompt

Use case: stylized-concept. Asset type: a single Minecraft inventory item sprite on an actual transparent background. Subject: one 'Shard of Space' from a fractured anomalous stone block, a narrow jagged elongated sliver pointing diagonally from lower left to upper right, broken dark gray slate and blue-gray stone edging with a blazing electric cyan and pale blue crystalline rift running through its center. Match chunky Minecraft pixel-art aesthetic, 32x32 logical pixel grid, crisp square pixel steps, nearest-neighbor enlarged pixels, no anti-aliasing. Keep a clear readable silhouette with two uneven chipped edges and tiny cyan protrusions, only one solid main shard. Palette charcoal, slate gray, muted steel blue, saturated electric blue, bright cyan, near-white blue core. Centered with transparent padding around all edges, fills about 80% of the square, no cast shadow, no ground, no frame, no text, no labels, no detached ornaments, no cube, no concept sheet. A flat 2D inventory sprite, not a rendered scene.

## Parallel universe materials

The 400 16×16 rock-face PNGs under `textures/block/universe/` and 30 resource-item PNGs were created algorithmically for HBK by `scripts/generate_universe_assets.py` (Python standard library, deterministic seed, original periodic motifs). They use no external images or copied vanilla textures. Block/item models, loot and recipes are generated by the same script.

The original parallel-universe botanical art consists of ten family atlases and a ground-top atlas under `art/universe_botany/`, created with the built-in `image_gen` tool. Full prompts are preserved in `prompts.json` and `soil-prompt.json` in that directory. `scripts/generate_universe_botany.py` mechanically exports 140 native-resolution 16×16 PNGs with nearest-neighbour sampling and binary cutout alpha; it also generates models, loot and recipes. These are original bark, wood, foliage, soil, plant and food designs following Minecraft's pixel-art conventions. No vanilla texture PNGs are copied or overwritten. Ordinary ores, aquatic accents and generic model geometry still reference Minecraft's built-in resources.

The selected original terrain-transition atlas `art/universe_botany/subsoils-v2.png` was generated with the built-in `image_gen` tool; the complete prompt is in `subsoil-v2-prompt.json`. It supplies ten plain subsoils and ten turf edges. The first draft and its prompt are retained separately and are not consumed. The exporter now produces 150 botanical/soil PNGs. The deterministic rock source was revised to use restrained clustered pixels and palettes that fit those ecosystems; no vanilla image files were copied.

The parallel mineral equipment added in `scripts/generate_universe_equipment.py` references Minecraft's built-in iron item models and iron humanoid equipment textures with constant mineral colours. No Minecraft PNGs were copied into HBK and no generated bitmap art is used for these 90 items. Geometry, UVs, item silhouettes and rendering are vanilla; equipment statistics match the vanilla diamond materials.

## Parallel universe alloys

The two original source atlases in `art/universe_alloys/metal-atlas.png` and `equipment-atlas.png` were generated with the built-in `image_gen` tool. Their complete prompts are preserved in `art/universe_alloys/prompts.json`. `scripts/generate_universe_alloy_textures.py` exports 135 material textures (45 samples, ingots and storage blocks) and 15 equipment inventory sprites at native 16×16 resolution, using nearest-neighbour sampling, binary alpha and six-tone material palettes. `scripts/generate_universe_alloys.py` also exports the JSON models and recipes. These replace the earlier single tinted vanilla raw-iron/iron-ingot models. Equipped armour retains vanilla humanoid geometry and references the built-in iron equipment texture with the alloy colour; its netherite statistics are unchanged. Machines use built-in block models; shields, crossbows, compasses, the clock, bundle and cargo minecart retain native rendering. No Minecraft PNGs were copied, and neither source atlas is a runtime dependency. Export requires Python + Pillow and uses only checked-in inputs.
