# Иконка «Опьянение»

Игровой ассет: `src/main/resources/assets/hbk/textures/mob_effect/beer.png`,
18×18 RGBA. Создан встроенным imagegen, затем обрезан по непрозрачному
силуэту и уменьшен ближайшим соседом с прозрачным полем в один пиксель.
В сборке используется только готовый PNG; исходное изображение не является
зависимостью проекта.

Для импорта другого исходного PNG:

```bash
python3 scripts/import_beer_effect_icon.py /path/to/transparent-source.png
```

## Промпт

Use case: stylized-concept. Asset type: tiny Minecraft status-effect UI sprite for Intoxication. Primary request: one original square pixel-art icon of a golden amber beer tankard with cream foam and a small curled dizziness spiral. Transparent background, no panel or border around the sprite. The icon must read crisply at an 18 by 18 pixel in-game size: use a strict coarse 18x18 pixel grid, square pixels, very simple silhouette, dark brown outline, amber/yellow beer, ivory foam, subtle mint dizziness curl. Center it within the canvas with a one-pixel transparent margin. Flat 2D retro Minecraft effect-icon aesthetic, no smooth edges or antialiasing, no gradients, no text, letters, logos, watermark, checkerboard pattern, floor or shadow. Deliver a single isolated sprite, not a sheet, not a mockup.

Параметр генерации: `transparent_background: true`.
