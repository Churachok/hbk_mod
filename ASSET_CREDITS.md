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
