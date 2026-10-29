# Сова на скакалке: текстура и импорт

Оригинальный атлас материалов 3D-модели создан встроенным `image_gen`, не через API/CLI. Фиолетовая сова и светлые глаза отсылают к [карте-предзнаменованию из мема](https://www.parents.ru/stil-zhizni/chto-znachit-populyarnyi-v-socsetyakh-mem-sova-so-skakalkoi-i-stoit-li-ego-boyatsya-id7163857/). По отдельному запросу пользователя к сцене добавлена именно исходная иллюстрация карты; она не является частью сгенерированного атласа и не выдаётся за оригинальную работу HBK.

- Сохранённый исходник: `art/skipping_owl/material-atlas.png`.
- Игровой ассет: `src/main/resources/assets/hbk/textures/entity/skipping_owl.png` (128×128, непрозрачный RGB).
- Импорт: `python3 scripts/import_skipping_owl_texture.py` (Pillow). Только нормализация размера методом nearest-neighbor, без дорисовки.
- Сборка и игра используют готовый PNG, не обращаются к генератору изображений, сети или личным каталогам.

Четыре квадранта по 64×64 логических пикселя: сверху слева фиолетовые перья, сверху справа светлые перья, снизу слева оранжевые клюв/лапки/ручки, снизу справа тёмные зрачки/верёвка. Глаза, клюв, крылья, лапки и скакалка — отдельная геометрия `SkippingOwlModel`, не нарисованные на корпусе детали. Модель использует UV внутри соответствующего квадранта. Анимация вращает сегменты скакалки и приподнимает сову в момент прохода шнура под лапами.

## Экранная карта и смех

- Источник изображения: [страница мода `owl jumping rope (update All animal cards)` на GameBanana](https://gamebanana.com/mods/691876), [прямое изображение](https://images.gamebanana.com/img/ss/mods/6a4ab8cd1bfdb.jpg). Это изображение карты, а не заново нарисованная версия.
- Сохранённый неизменённый JPEG: `art/skipping_owl/original-card.jpg`; SHA-256: `bc1717b9de769fceab2a7066bc1c3d7d3e2d350ea8b53afce78e032e16bb6882`.
- Игровой PNG: `assets/hbk/textures/gui/skipping_owl_card.png`, 1862×2048 RGB. Конверсия формата не меняет композицию, размер или пропорции. Размытие, вспышки и непрозрачность выполняются клиентским кодом/шейдером при рендере.
- Карта добавлена по явному запросу пользователя. Права на исходную иллюстрацию принадлежат её правообладателям; свободная лицензия на неё не подтверждена, и лицензия CC0 на смех **не распространяется на карту**.
- `assets/hbk/sounds/entity/skipping_owl/laugh.ogg` — отдельная восьмисекундная производная уже включённого `canned_laughter.ogg`: понижение высоты и темпа до 70%, эхо, ограничение пиков и плавные края. Исходная запись **Sitcom Laughter 9x, Small Audience**, Kinoton, [Freesound](https://freesound.org/people/Kinoton/sounds/383207/), CC0 1.0. Оригинальный звук смешной кнопки не изменён.
- Воспроизводимый импорт обоих готовых ассетов: `python3 scripts/prepare_skipping_owl_screen_assets.py` (Pillow, FFmpeg/ffprobe). Используются только исходники в репозитории; игра и сборка не требуют ни сети, ни этих программ. Проверяется длительность смеха ровно 8 секунд, соответствующая серверным 160 тикам.

## Полный промпт атласа 3D-модели

```text
Use case: stylized-concept. Asset type: game character MATERIAL TEXTURE ATLAS, not a character illustration. Primary request: one flat square opaque pixel-art UV material atlas for a Minecraft-style purple owl jumping rope. Composition: exact 2 by 2 grid of four equal square tiles meeting precisely at the center; no gutters, borders, labels or text. Entire upper-left quadrant: medium saturated violet/plum owl feathers with sparse tiny angular lavender feather chevrons, even fairly uniform purple coverage, no face or eyes. Entire upper-right quadrant: very pale warm cream with a light lavender tint, subtle sparse pixel feather flecks, mostly uniform light coverage. Entire lower-left quadrant: warm golden orange, subtle restrained pixel shading, mostly uniform orange coverage, material for beak, feet and jump rope handles. Entire lower-right quadrant: extremely dark charcoal-purple, nearly black, subtle pixel shading, material for pupils and jump rope cord. Style: crisp low-resolution pixel art, restrained 16-color palette, nearest-neighbor hard pixel edges, tiny repeating surface motifs. Neutral flat unlit material colors, no highlights or shadows across the atlas. Constraints: exactly these four material tiles, edges completely filled and opaque, no transparent background, no 3D render, no actual animal drawing, no eyes, no wings, no rope illustration, no sample cube, no palette swatches, no watermark. The 3D model supplies every shape; this image supplies only material colors and feather micro-patterns.
```
