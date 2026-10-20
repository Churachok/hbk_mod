# Иконка «Егерское чутьё»

Рисунки созданы встроенным **imagegen** 16 октября 2026 года.
Параметр всех вызовов — `transparent_background: true`.
CLI, API-ключи и внешние генераторы не использовались.
Начальная генерация: `exec-11f10bcc-8275-4245-ab29-f72c4a48198b`.
Затем выполнены две точечные правки размера креста:
`exec-163deec7-14dd-427b-bde3-df248edae308` и окончательная
`exec-37ea8450-44a1-4dc5-a6bc-ecb09a5347f9` (исходник v3).
Сам олень, палитра и прозрачность сохранялись. Эти варианты и их
промпты ниже документируют первоначальную версию.

Для более качественного значка эффекта выполнена одна дополнительная
правка v3: `exec-bd8a8e5d-5586-41da-92af-b6172a1ddd4b` (исходник v4).
Она добавила более аккуратные мелкие пиксельные детали, сохранив
фронтальную голову, симметричные рога, центральный крест и палитру.
Только HUD использует v4; рисунок rescue-анимации остался прежним v3.

Перед генерацией просмотрена наша игровая текстура
`src/main/resources/assets/hbk/textures/item/jagermeister_bottle.png`:
из неё взяты мотив головы оленя с крестом и кремово-золотистая,
коричневая и тёмно-зелёная палитра. Исходная текстура не изменялась;
новый рисунок не содержит бутылки, этикетки, текста или медальона.
Локальные внешние референсы не требуются.

## Файлы

- `docs/assets/hunters-instinct-source-v4.png` — неизменённый результат улучшения значка встроенным imagegen, 1254×1254 RGBA; источник текущего HUD.
- `docs/assets/hunters-instinct-source-v3.png` — неизменённый результат первоначальной серии правок, 1254×1254 RGBA; источник прежней rescue-анимации.
- `docs/assets/hunters-instinct-source.png` — сохранённый начальный вариант с меньшим крестом; не используется в игре или импорте по умолчанию.
- `src/main/resources/assets/hbk/textures/mob_effect/hunters_instinct.png` — иконка эффекта, 64×64 RGBA, рисунок вписан в область 60×60 с прозрачным полем минимум два пикселя; в HUD отображается в стандартном размере эффекта.
- `src/main/resources/assets/hbk/textures/item/jagermeister_stag.png` — версия для анимации, 128×128 RGBA, прозрачное поле в восемь пикселей.

В игру и JAR входят только два готовых игровых PNG.
Документированный исходник нужен только для повторения импорта,
не для обычной сборки или запуска Minecraft.

## Повторение импорта

```bash
python3 scripts/import_hunters_instinct_icon.py
```

Нужен Pillow. По умолчанию источники независимы: v4 для HUD и v3 для
анимации. Их можно указать отдельно:

```bash
python3 scripts/import_hunters_instinct_icon.py \
  --hud-source docs/assets/hunters-instinct-source-v4.png \
  --animation-source docs/assets/hunters-instinct-source-v3.png
```

Скрипт определяет область обрезки по alpha ≥ 128, затем использует
только обрезку, уменьшение ближайшим соседом и центрирование
на прозрачном холсте. Alpha внутри обрезанного изображения сохраняется;
контур, цвет и детали не дорисовываются и не перекрашиваются кодом.
Прежняя иконка HUD была 18×18 с рисунком 17×17: нечётная ширина
сохраняла центральный штрих креста при уменьшении. Теперь её заменяет
более детальная 64-пиксельная версия. Алгоритм импорта анимации
не изменён: v3 вписывается в 112×112 внутри холста 128×128.
Её SHA-256 остаётся
`aa870a170c1883b89ef3a7879c07c76987323c6a9f4cc468bd9c43169a09cd76`.
Повторный импорт не перезаписывает побайтно совпадающие файлы.
Генерация рисунка не вызывается из скрипта импорта.

## Улучшение качества HUD, v4

Встроенный imagegen получил v3 как единственную редактируемую картинку,
с `transparent_background: true`. Сохранённый v4 — оригинальный экспорт
этого вызова. Финальный PNG получен только механическим импортом,
без перерисовки, перекрашивания или изменения спрайта анимации.

```text
Use case: style-transfer
Asset type: improved transparent Minecraft HUD status-effect icon, Hunter's Instinct.
Input image: Image 1 is the EDIT TARGET, our existing golden stag head with symmetrical antlers and a cream cross between them.
Primary request: make this same icon noticeably cleaner and a little higher quality. Re-render the stag head with more refined, intentional pixel-art detail on an approximately64x64 logical pixel grid instead of the old very chunky18x18-style blocks. Preserve the recognizable front-facing head, full symmetrical branching antlers, upright central cross and existing gold/cream/dark-forest-green/brown palette. It must look like an upgraded version of the same icon, not a different symbol.
Style: polished hand-crafted game HUD pixel art. Smaller carefully placed pixel clusters, clearer antler branching, more natural but still sharp pixel-stepped curves, balanced symmetrical facial planes, clean dark eyes and muzzle, restrained cream highlights and ochre/brown shading. Crisp readable silhouette; no noisy texture, no photographic fur, no3D render. Edges can be refined and smoother in shape, but keep controlled pixel-art detail rather than blurred or painterly strokes.
Composition: isolated centered stag head and antlers only, square canvas, all antler tips included with a small transparent margin. Keep the cream Christian cross clearly visible in the negative space between the antlers, thick and simple enough to survive display at18x18; do not hide it in fine detail. Strong contrast on both dark and light HUD backgrounds.
Invariants: same frontal deer identity, roughly the same silhouette/proportions, symmetrical antlers, central cross, golden-cream face, deep-green/brown outline, genuinely transparent alpha background. This is only the status-icon artwork, not an animation change.
Avoid: bottle, label, medallion, circular frame, badge, text, letters, numbers, scene, scenery, ground, drop shadow, glow, watermark, checkerboard background, multiple sprites or a sprite sheet.
```

## Исходный промпт

```text
Use case: stylized-concept
Asset type: one transparent Minecraft status-effect pixel-art icon, also used as a floating game sprite, called Hunter's Instinct.
Primary request: a single isolated frontal stag head with broad perfectly symmetric branching antlers and a very small cream cross centered between the antlers, inspired by the stag motif on a Jagermeister bottle label. The head and antlers alone are the entire artwork.
Reference guidance: our bottle label motif uses ivory antlers, a golden-cream/tan deer face, a brown nose and deep forest-green accents. Preserve that recognizable head-and-cross idea without including the surrounding label.
Scene/backdrop: genuinely transparent alpha background, no backdrop of any kind.
Style/medium: strict coarse Minecraft pixel art designed to remain legible when reduced to 18 by 18 pixels. Build the entire silhouette as if on an 18x18 logical pixel grid: large square pixels, limited flat palette, chunky square-edged outlines, simple dark eyes and nose. No smoothing, antialiasing, gradients or fine detail. This should be an enlarged pixel sprite, not a detailed illustration.
Composition/framing: frontal centered head, symmetric antlers fully inside the square canvas, small cross in the open negative space between the antlers, one logical pixel of transparent breathing room on every side. Bold silhouette and recognizable muzzle; tiny cream cross must be clearly separate from antlers.
Color palette: warm golden cream for face and antlers, restrained ochre highlights, dark forest-green and dark-brown outline/shading, readable on light and dark game backgrounds.
Constraints: exactly one sprite. No bottle, body, text, lettering, numbers, wordmark, label, medallion, circle, badge, panel, border around the canvas, shadow, ground, glow, watermark or checkerboard pattern. Transparent pixels must stay genuinely transparent. Not a sprite sheet. Do not display any size labels or captions.
```

## Первая точечная правка

```text
Use case: precise-object-edit
Input image: Image 1 is the EDIT TARGET, the existing isolated transparent pixel-art golden stag head with symmetric antlers and a small cross between them.
Primary request: CHANGE ONLY THE SIZE OF THE CREAM CROSS in the empty space between the antlers. Enlarge that cross to approximately 20 percent of the entire stag silhouette width and 26 percent of the silhouette height. Keep it a simple thick plus-like Christian cross: horizontal bar three equal square logical pixels wide, vertical stem four equal square logical pixels tall, every stroke one of those logical pixels thick. This is essential because the complete sprite is reduced to only18x18 in Minecraft; the previous tiny cross vanished entirely when downsampled. The cross needs to occupy roughly3x4pixels in that tiny final icon.
Position: center the larger cross in the existing open dark/transparent space between the inner antler branches, above the forehead. Keep a transparent gap around it, do not touch the antlers or forehead. Use the same warm cream fill and restrained dark-green outline as the existing cross.
Invariants: the stag head, antler shape, head size and position, square canvas, golden/cream/brown/forest-green palette, sharp square pixel-art style and genuinely transparent background must remain unchanged. Do not enlarge or redesign the deer. Do not introduce a bottle, text, label, circle, medallion, background, shadow, watermark or any other object. Make only the cross larger and thick enough to survive18x18 nearest-neighbour reduction.
```

## Финальная точечная правка

```text
Use case: precise-object-edit
Input image: Image 1 is the edit target, an isolated transparent pixel-art stag head.
Change ONLY the cross between the antlers. The existing cross is still much too small for the final tiny18x18 game icon. Make that cross THREE TIMES WIDER and about ONE AND A HALF TIMES TALLER. Give it a thick cream vertical stroke and long thick cream horizontal arms. The new cross should almost fill the open horizontal gap between the two nearest upright antler branches, but leave a little transparent space before touching them. It should be approximately as wide as the stag's muzzle and clearly larger than either eye. Use a bold blocky Christian-cross shape, not a thin decorative mark. Dark forest-green square outline, cream interior. It is crucial that its horizontal arms visibly extend well to both sides of its vertical stroke after extreme thumbnail reduction.
Keep every other part unchanged: the same front-facing stag, the same full antlers and ears, the same golden and cream head, dark eyes and muzzle, same forest-green outline, same square framing, truly transparent alpha background. Do not resize the stag, do not change the canvas framing, and do not add a background, circle, medallion, words, bottle or additional decoration.
```
