#!/usr/bin/env python3
"""
Paper-doll sprite sheet helper for Darkest Pixel Dungeon.

Supports:
  1. guide   - Export composited hero reference strips for drawing hats or
               arbitrary-size weapons in Aseprite.
  2. hat     - Place a 1-frame (12x15) or multi-frame hat sprite into a row of
               helmets.png with automatic per-frame head offset alignment.
  3. weapon  - Save a custom-sized weapon (.png + .json with custom frames/FPS)
               into actors/hero/weapons/<name>.* OR pack a 20x19 strip into
               legacy weapons.png.
  4. extract - Extract a single row from helmets.png or weapons.png to a PNG.
  5. preview - Render a 6x nearest-neighbor GIF or contact-sheet PNG combining
               body + head + armor + helmet + (legacy or custom) weapon.

Requires: Pillow (pip install pillow)
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Dict, List, Optional, Tuple

from PIL import Image, ImageDraw, ImageOps

def _find_repo_root() -> Path:
    cur = Path(__file__).resolve().parent
    for parent in [cur, *cur.parents]:
        if (parent / "core/src/main/assets/actors/hero").is_dir():
            return parent
    return cur.parent.parent


REPO_ROOT = _find_repo_root()
HERO_DIR = REPO_ROOT / "core/src/main/assets/actors/hero"
CUSTOM_WEAP_DIR = HERO_DIR / "weapons"

FRAME_W, FRAME_H = 12, 15
SHEET_W = 256
NUM_FRAMES = 21

WEAP_W, WEAP_H = 20, 19
WEAP_COLS = 5

# Per-frame head transform (dx, dy, flip_h) relative to frame 0, derived from head.png/body.png.
# Frame 1 (idle head-turn) mirrors the head horizontally and sits at x=[1..11], so mirroring
# a 12x15 frame-0 sprite (x=[2..12] -> [0..10]) requires dx=+1 to stay aligned to the head.
# None means head is not shown (death frames 8..12).
HEAD_OFFSETS: List[Optional[Tuple[int, int, bool]]] = [
    (0, 0, False),   # 0: idle 0
    (1, 0, True),    # 1: idle 1 (head turned/mirrored)
    (0, 0, False),   # 2: run 0
    (0, 0, False),   # 3: run 1
    (0, 1, False),   # 4: run 2
    (0, 0, False),   # 5: run 3
    (0, 0, False),   # 6: run 4
    (0, 1, False),   # 7: run 5
    None,            # 8: die 0
    None,            # 9: die 1
    None,            # 10: die 2
    None,            # 11: die 3
    None,            # 12: die 4
    (-1, 0, False),  # 13: attack 0
    (0, 0, False),   # 14: attack 1
    (1, 0, False),   # 15: attack 2
    (0, 0, False),   # 16: operate 0
    (0, 0, False),   # 17: operate 1
    (0, 0, False),   # 18: fly 0
    (0, 0, False),   # 19: read 0
    (0, 0, False),   # 20: read 1
]

# Matches DollSprite.kt animations: (name, body_frames, fps, weapon_frames, weapon_dx, weapon_dy)
ANIMATIONS = [
    ("idle",    [0, 0, 0, 1, 0, 0, 1, 1], 1,  [0, 0, 0, 0, 0, 0, 0, 0], [2, 2, 2, 2, 2, 2, 2, 2], [0]*8),
    ("run",     [2, 3, 4, 5, 6, 7],       20, [0, 0, 1, 1, 0, 0],       [2, 2, 3, 3, 2, 2],       [0]*6),
    ("attack",  [13, 14, 15, 0],          15, [1, 2, 3, 0],             [1, 2, 5, 3],             [0]*4),
    ("operate", [16, 17, 16, 17],         8,  [0, 0, 0, 0],             [2, 2, 2, 2],             [0]*4),
    ("fly",     [18],                     1,  [0],                      [3],                      [0]),
    ("read",    [19, 20, 20, 20, 19],     20, None,                     None,                     None),
    ("die",     [8, 9, 10, 11, 12, 11],   20, None,                     None,                     None),
]

HERO_CLASSES = ["warrior", "mage", "rogue", "huntress", "sorceress", "exile"]


def load_sheet(name: str) -> Image.Image:
    return Image.open(HERO_DIR / name).convert("RGBA")


def crop_cell(sheet: Image.Image, row: int, col: int, w: int = FRAME_W, h: int = FRAME_H) -> Image.Image:
    return sheet.crop((col * w, row * h, (col + 1) * w, (row + 1) * h))


def paste_with_offset(dst: Image.Image, src: Image.Image, x: int, y: int) -> None:
    """Alpha-composite src onto dst at (x, y), clipping to dst bounds."""
    tmp = Image.new("RGBA", dst.size, (0, 0, 0, 0))
    tmp.paste(src, (x, y))
    dst.alpha_composite(tmp)


def shift_cell(sheet: Image.Image, row: int, col: int, dx: int, dy: int, w: int = FRAME_W, h: int = FRAME_H) -> None:
    """Shift a single (row, col) cell's pixels by (dx, dy) in-place."""
    cell = crop_cell(sheet, row, col, w, h)
    shifted = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    paste_with_offset(shifted, cell, dx, dy)
    sheet.paste(shifted, (col * w, row * h))


def armor_row(hero_class_idx: int, tier: int) -> int:
    if tier >= 6:
        return 7 + hero_class_idx
    if tier == 0:
        # huntress (3), sorceress (4) use female underwear row 1
        return 1 if hero_class_idx in (3, 4) else 0
    return tier + 1


HELMET_META_PATH = HERO_DIR / "helmets_meta.json"


def load_helmet_meta() -> Dict:
    if HELMET_META_PATH.exists():
        return json.loads(HELMET_META_PATH.read_text(encoding="utf-8"))
    return {"defaultOffset": [1, -1], "defaultFrameDeltas": {}, "rows": {}}


def save_helmet_meta(meta: Dict) -> None:
    HELMET_META_PATH.write_text(json.dumps(meta, indent=2) + "\n", encoding="utf-8")


def mirror_if_head_turn(dx: int, dy: int, frame_idx: int) -> Tuple[int, int]:
    return (1 - dx, dy) if frame_idx == 1 else (dx, dy)


def helmet_offset(meta: Optional[Dict], row: int, frame_idx: int) -> Tuple[int, int]:
    if meta is None:
        meta = load_helmet_meta()
    key = str(frame_idx)
    row_cfg = meta.get("rows", {}).get(str(row))
    if row_cfg and key not in row_cfg.get("frameDeltas", {}):
        fo = row_cfg.get("frameOffsets", {}).get(key)
        if fo is not None:
            return int(round(fo[0])), int(round(fo[1]))
    raw_base = (row_cfg.get("offset") if row_cfg else None) or meta.get("defaultOffset", [1, -1])
    bx, by = mirror_if_head_turn(int(round(raw_base[0])), int(round(raw_base[1])), frame_idx)
    delta = (row_cfg.get("frameDeltas", {}).get(key) if row_cfg else None) or meta.get("defaultFrameDeltas", {}).get(key)
    if delta is not None:
        return bx + int(round(delta[0])), by + int(round(delta[1]))
    if not row_cfg or row_cfg.get("offset") is None:
        dfo = meta.get("defaultFrameOffsets", {}).get(key)
        if dfo is not None:
            return int(round(dfo[0])), int(round(dfo[1]))
    return bx, by


def helmet_render_head(meta: Optional[Dict], row: Optional[int]) -> bool:
    if row is None:
        return True
    if meta is None:
        meta = load_helmet_meta()
    row_cfg = meta.get("rows", {}).get(str(row))
    if row_cfg and "renderHead" in row_cfg:
        return bool(row_cfg["renderHead"])
    return True


def compose_hero_frame(
    body: Image.Image,
    head: Image.Image,
    armor: Image.Image,
    helmets: Optional[Image.Image],
    weapons: Optional[Image.Image],
    hero_class_idx: int,
    tier: int,
    helmet_row: Optional[int],
    weapon_row: Optional[int],
    body_col: int,
    weap_col: Optional[int] = None,
    weap_dx: int = 0,
    weap_dy: int = 0,
    pad: int = 8,
    helmet_meta: Optional[Dict] = None,
) -> Image.Image:
    """Compose a single hero frame on a padded canvas using DollSprite's exact math."""
    canvas = Image.new("RGBA", (FRAME_W + pad * 2, FRAME_H + pad * 2), (0, 0, 0, 0))
    canvas.alpha_composite(crop_cell(body, 0, body_col), (pad, pad))
    show_head = True
    if helmets is not None and helmet_row is not None and (helmet_row + 1) * FRAME_H <= helmets.height:
        show_head = helmet_render_head(helmet_meta, helmet_row)
    if show_head:
        canvas.alpha_composite(crop_cell(head, hero_class_idx, body_col), (pad, pad))
    canvas.alpha_composite(crop_cell(armor, armor_row(hero_class_idx, tier), body_col), (pad, pad))

    if helmets is not None and helmet_row is not None:
        if (helmet_row + 1) * FRAME_H <= helmets.height:
            hx, hy = helmet_offset(helmet_meta, helmet_row, body_col)
            paste_with_offset(canvas, crop_cell(helmets, helmet_row, body_col), pad + hx, pad + hy)

    if weapons is not None and weapon_row is not None and weap_col is not None:
        if (weapon_row + 1) * WEAP_H <= weapons.height:
            # WeaponLayer: center(owner.center()) -> x = owner.x + 6 - 10 + dx, y = owner.y + 7.5 - 9.5 + dy
            wx = pad + (FRAME_W // 2) - (WEAP_W // 2) + weap_dx
            wy = pad + int(FRAME_H / 2.0 - WEAP_H / 2.0) + weap_dy
            paste_with_offset(canvas, crop_cell(weapons, weapon_row, weap_col, WEAP_W, WEAP_H), wx, wy)

    return canvas


def compose_custom_weapon(
    canvas: Image.Image,
    weap_sheet: Image.Image,
    fw: int,
    fh: int,
    weap_col: int,
    dx: float,
    dy: float,
    pad: int,
) -> None:
    cols = max(1, weap_sheet.width // fw)
    r, c = divmod(weap_col, cols)
    cell = crop_cell(weap_sheet, r, c, fw, fh)
    wx = pad + int(round(FRAME_W / 2.0 - fw / 2.0 + dx))
    wy = pad + int(round(FRAME_H / 2.0 - fh / 2.0 + dy))
    paste_with_offset(canvas, cell, wx, wy)


def parse_anim_spec(spec_str: str, default_fps: float, default_ox: float, default_oy: float, looped: bool) -> Dict:
    """Parse '0,1,2,3@15' or '0,1@8' into a JSON animation entry."""
    if "@" in spec_str:
        frames_part, fps_part = spec_str.split("@", 1)
        fps = float(fps_part)
    else:
        frames_part = spec_str
        fps = default_fps
    frames = [int(x.strip()) for x in frames_part.split(",") if x.strip()]
    return {
        "frames": frames,
        "fps": fps,
        "looped": looped,
        "offsetX": default_ox,
        "offsetY": default_oy,
    }


def cmd_guide(args: argparse.Namespace) -> None:
    hc = HERO_CLASSES.index(args.hero_class)
    body = load_sheet("body.png")
    head = load_sheet("head.png")
    armor = load_sheet("armor.png")

    if args.type == "hat":
        out = Image.new("RGBA", (SHEET_W, FRAME_H), (0, 0, 0, 0))
        for col in range(NUM_FRAMES):
            cell = compose_hero_frame(body, head, armor, None, None, hc, args.tier, None, None, col, pad=0)
            out.alpha_composite(cell, (col * FRAME_W, 0))
    else:
        fw = args.frame_width or WEAP_W
        fh = args.frame_height or WEAP_H
        # Weapon guide: 5 sample poses (idle, run, attack 0/1/2) at arbitrary frame size
        samples = [
            (0, 2, 0),   # col 0: idle
            (4, 3, 0),   # col 1: run
            (13, 1, 0),  # col 2: attack 0
            (14, 2, 0),  # col 3: attack 1
            (15, 5, 0),  # col 4: attack 2
        ]
        out = Image.new("RGBA", (fw * len(samples), fh), (0, 0, 0, 0))
        for wcol, (bcol, dx, dy) in enumerate(samples):
            hx = int(round(fw / 2.0 - FRAME_W / 2.0 - dx))
            hy = int(round(fh / 2.0 - FRAME_H / 2.0 - dy))
            hero_cell = compose_hero_frame(body, head, armor, None, None, hc, args.tier, None, None, bcol, pad=0)
            cell = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
            paste_with_offset(cell, hero_cell, hx, hy)
            out.alpha_composite(cell, (wcol * fw, 0))

    out_path = Path(args.output)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out.save(out_path)
    print(f"Saved {args.type} guide ({out.size[0]}x{out.size[1]}) to {out_path}")


def cmd_hat(args: argparse.Namespace) -> None:
    src = Image.open(args.input).convert("RGBA")
    src_frames = max(1, src.width // FRAME_W)
    if src.height < FRAME_H:
        raise ValueError(f"Input height {src.height} < {FRAME_H}")

    row_img = Image.new("RGBA", (SHEET_W, FRAME_H), (0, 0, 0, 0))
    for col in range(NUM_FRAMES):
        if src_frames >= NUM_FRAMES:
            # Already a full 21-frame strip: copy each frame as-is
            hat_frame = crop_cell(src, 0, col)
            paste_with_offset(row_img, hat_frame, col * FRAME_W, 0)
            continue
        transform = HEAD_OFFSETS[col]
        if transform is None:
            continue
        dx, dy, flip_h = transform
        src_idx = col % src_frames if src_frames > 1 else 0
        hat_frame = crop_cell(src, 0, src_idx)
        if flip_h:
            hat_frame = ImageOps.mirror(hat_frame)
        paste_with_offset(row_img, hat_frame, col * FRAME_W + dx, dy)

    sheet_path = Path(args.sheet)
    if sheet_path.exists():
        sheet = Image.open(sheet_path).convert("RGBA")
    else:
        sheet = Image.new("RGBA", (SHEET_W, FRAME_H), (0, 0, 0, 0))

    needed_h = (args.row + 1) * FRAME_H
    if sheet.height < needed_h:
        expanded = Image.new("RGBA", (max(sheet.width, SHEET_W), needed_h), (0, 0, 0, 0))
        expanded.paste(sheet, (0, 0))
        sheet = expanded

    # Clear target row before pasting
    clear_box = Image.new("RGBA", (SHEET_W, FRAME_H), (0, 0, 0, 0))
    sheet.paste(clear_box, (0, args.row * FRAME_H))
    sheet.paste(row_img, (0, args.row * FRAME_H))
    sheet.save(sheet_path)
    print(f"Updated row {args.row} in {sheet_path} (source frames: {src_frames}, sheet size: {sheet.size})")


def cmd_weapon(args: argparse.Namespace) -> None:
    src = Image.open(args.input).convert("RGBA")

    if args.row is not None:
        # Legacy 20x19 row in weapons.png
        if src.height < WEAP_H:
            raise ValueError(f"Legacy weapon row requires height >= {WEAP_H}, got {src.height}")
        row_img = Image.new("RGBA", (WEAP_W * WEAP_COLS, WEAP_H), (0, 0, 0, 0))
        row_img.paste(src.crop((0, 0, min(src.width, WEAP_W * WEAP_COLS), WEAP_H)), (0, 0))

        sheet_path = Path(args.sheet)
        if sheet_path.exists():
            sheet = Image.open(sheet_path).convert("RGBA")
        else:
            sheet = Image.new("RGBA", (WEAP_W * WEAP_COLS, WEAP_H), (0, 0, 0, 0))

        needed_h = (args.row + 1) * WEAP_H
        if sheet.height < needed_h:
            expanded = Image.new("RGBA", (max(sheet.width, WEAP_W * WEAP_COLS), needed_h), (0, 0, 0, 0))
            expanded.paste(sheet, (0, 0))
            sheet = expanded

        clear_box = Image.new("RGBA", (WEAP_W * WEAP_COLS, WEAP_H), (0, 0, 0, 0))
        sheet.paste(clear_box, (0, args.row * WEAP_H))
        sheet.paste(row_img, (0, args.row * WEAP_H))
        sheet.save(sheet_path)
        print(f"Updated legacy weapon row {args.row} in {sheet_path} (size: {sheet.size})")
        return

    if not args.name:
        raise ValueError("Provide either --name <custom_name> or --row <legacy_row>")

    fw = args.frame_width or src.width
    fh = args.frame_height or src.height
    if src.width % fw != 0 or src.height % fh != 0:
        print(f"Warning: image {src.size} is not an exact multiple of {fw}x{fh}")
    total_frames = max(1, (src.width // fw) * (src.height // fh))

    CUSTOM_WEAP_DIR.mkdir(parents=True, exist_ok=True)
    out_png = CUSTOM_WEAP_DIR / f"{args.name}.png"
    out_json = CUSTOM_WEAP_DIR / f"{args.name}.json"
    src.save(out_png)

    ox, oy = args.offset_x, args.offset_y
    default_seq = ",".join(str(i) for i in range(total_frames))
    anims: Dict[str, Dict] = {
        "idle": parse_anim_spec(args.idle or f"0@{args.fps}", args.fps, ox, oy, True),
        "run": parse_anim_spec(args.run or f"{default_seq}@{args.fps}", args.fps, ox, oy, True),
        "attack": parse_anim_spec(args.attack or f"{default_seq}@{args.fps}", args.fps, ox, oy, False),
        "operate": parse_anim_spec(args.operate or f"0@{args.fps}", args.fps, ox, oy, False),
        "fly": parse_anim_spec(args.fly or f"0@{args.fps}", args.fps, ox, oy, True),
    }
    for item in (args.extra or []):
        if "=" not in item:
            raise ValueError(f"--extra must be 'name=frames@fps', got: {item}")
        ename, espec = item.split("=", 1)
        looped = False
        if espec.endswith("!loop"):
            espec = espec[:-5]
            looped = True
        anims[ename.strip()] = parse_anim_spec(espec.strip(), args.fps, ox, oy, looped)
    spec = {
        "frameWidth": fw,
        "frameHeight": fh,
        "offsetX": ox,
        "offsetY": oy,
        "animations": anims,
    }
    out_json.write_text(json.dumps(spec, indent=2) + "\n", encoding="utf-8")
    print(f"Saved custom weapon '{args.name}' ({fw}x{fh}, {total_frames} frames) to {out_png} + {out_json}")


def cmd_extract(args: argparse.Namespace) -> None:
    if args.type == "hat":
        sheet = load_sheet("helmets.png")
        row_img = sheet.crop((0, args.row * FRAME_H, SHEET_W, (args.row + 1) * FRAME_H))
    else:
        sheet = load_sheet("weapons.png")
        row_img = sheet.crop((0, args.row * WEAP_H, WEAP_W * WEAP_COLS, (args.row + 1) * WEAP_H))

    out_path = Path(args.output)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    row_img.save(out_path)
    print(f"Extracted {args.type} row {args.row} to {out_path}")


def load_custom_weapon(name: str) -> Tuple[Image.Image, Dict]:
    png_path = CUSTOM_WEAP_DIR / f"{name}.png"
    json_path = CUSTOM_WEAP_DIR / f"{name}.json"
    if not png_path.exists() or not json_path.exists():
        raise FileNotFoundError(f"Missing custom weapon files: {png_path} / {json_path}")
    return Image.open(png_path).convert("RGBA"), json.loads(json_path.read_text(encoding="utf-8"))


def cmd_preview(args: argparse.Namespace) -> None:
    hc = HERO_CLASSES.index(args.hero_class)
    body = load_sheet("body.png")
    head = load_sheet("head.png")
    armor = load_sheet("armor.png")
    helmets = load_sheet("helmets.png") if args.helmet is not None else None

    weapon_row: Optional[int] = None
    weapons: Optional[Image.Image] = None
    custom_weap: Optional[Tuple[Image.Image, Dict]] = None
    if args.weapon is not None:
        if args.weapon.isdigit():
            weapon_row = int(args.weapon)
            weapons = load_sheet("weapons.png")
        else:
            custom_weap = load_custom_weapon(args.weapon)

    scale = args.scale
    pad = 12 if custom_weap is None else max(12, custom_weap[1].get("frameWidth", 20) // 2 + 4)
    cw, ch = FRAME_W + pad * 2, FRAME_H + pad * 2

    if args.format == "sheet":
        cols = 7
        rows = 3
        out = Image.new("RGBA", (cols * cw * scale, rows * ch * scale), (32, 32, 40, 255))
        draw = ImageDraw.Draw(out)
        for idx in range(NUM_FRAMES):
            w_col = 0 if weapons is not None else None
            w_dx = 2
            if idx in (4, 7):
                w_col, w_dx = 1, 3
            elif idx == 13:
                w_col, w_dx = 1, 1
            elif idx == 14:
                w_col, w_dx = 2, 2
            elif idx == 15:
                w_col, w_dx = 3, 5
            elif idx in (8, 9, 10, 11, 12, 19, 20):
                w_col = None

            comp = compose_hero_frame(
                body, head, armor, helmets, weapons,
                hc, args.tier, args.helmet, weapon_row,
                idx, w_col, w_dx, 0, pad=pad,
            )
            if custom_weap is not None and idx not in (8, 9, 10, 11, 12, 19, 20):
                wsheet, wspec = custom_weap
                fw, fh = wspec.get("frameWidth", WEAP_W), wspec.get("frameHeight", WEAP_H)
                anims = wspec.get("animations", {})
                state = "idle"
                if 2 <= idx <= 7:
                    state = "run"
                elif 13 <= idx <= 15:
                    state = "attack"
                elif idx in (16, 17):
                    state = "operate"
                elif idx == 18:
                    state = "fly"
                anim = anims.get(state) or anims.get("idle")
                if anim and anim.get("frames"):
                    f_idx = anim["frames"][0]
                    compose_custom_weapon(
                        comp, wsheet, fw, fh, f_idx,
                        anim.get("offsetX", wspec.get("offsetX", 0)),
                        anim.get("offsetY", wspec.get("offsetY", 0)),
                        pad,
                    )

            frame = comp.resize((cw * scale, ch * scale), Image.Resampling.NEAREST)
            gx, gy = (idx % cols) * cw * scale, (idx // cols) * ch * scale
            out.alpha_composite(frame, (gx, gy))
            draw.text((gx + 4, gy + 4), str(idx), fill=(220, 220, 220, 255))

        out_path = Path(args.output)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out.save(out_path)
        print(f"Saved contact sheet preview to {out_path}")
        return

    frames: List[Image.Image] = []
    durations: List[int] = []
    for anim_name, b_frames, fps, w_frames, w_dx_list, w_dy_list in ANIMATIONS:
        dur = max(60, int(1000 / fps))
        for i, bcol in enumerate(b_frames):
            wcol = w_frames[i] if w_frames else None
            wdx = w_dx_list[i] if w_dx_list else 0
            wdy = w_dy_list[i] if w_dy_list else 0
            comp = compose_hero_frame(
                body, head, armor, helmets, weapons,
                hc, args.tier, args.helmet, weapon_row,
                bcol, wcol, wdx, wdy, pad=pad,
            )
            if custom_weap is not None and anim_name not in ("read", "die"):
                wsheet, wspec = custom_weap
                fw, fh = wspec.get("frameWidth", WEAP_W), wspec.get("frameHeight", WEAP_H)
                anims = wspec.get("animations", {})
                anim = anims.get(anim_name) or anims.get("idle")
                if anim and anim.get("frames"):
                    wf_list = anim["frames"]
                    wf_fps = max(1.0, float(anim.get("fps", fps)))
                    # Sample custom weapon frame at current elapsed time in this animation
                    t_sec = i / float(fps)
                    wf_pos = int(t_sec * wf_fps) % len(wf_list)
                    fo = (anim.get("offsets") or [None] * len(wf_list))[wf_pos]
                    ox = fo[0] if fo else anim.get("offsetX", wspec.get("offsetX", 0))
                    oy = fo[1] if fo else anim.get("offsetY", wspec.get("offsetY", 0))
                    compose_custom_weapon(comp, wsheet, fw, fh, wf_list[wf_pos], ox, oy, pad)

            bg = Image.new("RGBA", comp.size, (32, 32, 40, 255))
            bg.alpha_composite(comp)
            frames.append(bg.resize((cw * scale, ch * scale), Image.Resampling.NEAREST))
            durations.append(dur)

    out_path = Path(args.output)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    frames[0].save(
        out_path,
        save_all=True,
        append_images=frames[1:],
        duration=durations,
        loop=0,
    )
    print(f"Saved animated GIF preview ({len(frames)} frames) to {out_path}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Paper-doll sprite helper for Darkest Pixel Dungeon")
    sub = parser.add_subparsers(dest="cmd", required=True)

    p_guide = sub.add_parser("guide", help="Export Aseprite reference guide PNG")
    p_guide.add_argument("--type", choices=["hat", "weapon"], required=True)
    p_guide.add_argument("--class", dest="hero_class", choices=HERO_CLASSES, default="warrior")
    p_guide.add_argument("--tier", type=int, default=2)
    p_guide.add_argument("--frame-width", type=int, default=None, help="Weapon guide frame width (default 20)")
    p_guide.add_argument("--frame-height", type=int, default=None, help="Weapon guide frame height (default 19)")
    p_guide.add_argument("-o", "--output", required=True)
    p_guide.set_defaults(func=cmd_guide)

    p_hat = sub.add_parser("hat", help="Pack a 1-frame or multi-frame hat into helmets.png")
    p_hat.add_argument("-i", "--input", required=True, help="12x15 (1F) or N*12x15 strip PNG")
    p_hat.add_argument("--row", type=int, required=True, help="Target row index in helmets.png")
    p_hat.add_argument("--sheet", default=str(HERO_DIR / "helmets.png"))
    p_hat.set_defaults(func=cmd_hat)

    p_weap = sub.add_parser("weapon", help="Save a custom weapon (.png+.json) or update a legacy weapons.png row")
    p_weap.add_argument("-i", "--input", required=True, help="Weapon sprite sheet PNG")
    p_weap.add_argument("--name", default=None, help="Custom weapon name (writes actors/hero/weapons/<name>.png+.json)")
    p_weap.add_argument("--row", type=int, default=None, help="Legacy row index in weapons.png (20x19)")
    p_weap.add_argument("--sheet", default=str(HERO_DIR / "weapons.png"))
    p_weap.add_argument("--frame-width", type=int, default=None)
    p_weap.add_argument("--frame-height", type=int, default=None)
    p_weap.add_argument("--fps", type=float, default=10.0)
    p_weap.add_argument("--offset-x", type=float, default=2.0)
    p_weap.add_argument("--offset-y", type=float, default=0.0)
    p_weap.add_argument("--idle", default=None, help="e.g. '0@1'")
    p_weap.add_argument("--run", default=None, help="e.g. '0,1@10'")
    p_weap.add_argument("--attack", default=None, help="e.g. '1,2,3,0@15'")
    p_weap.add_argument("--operate", default=None, help="e.g. '0@8'")
    p_weap.add_argument("--fly", default=None, help="e.g. '0@1'")
    p_weap.add_argument("--extra", action="append", default=[], help="Special anim, e.g. 'skill=2,3,4@15' (append '!loop' for looped)")
    p_weap.set_defaults(func=cmd_weapon)

    p_ext = sub.add_parser("extract", help="Extract a row from helmets.png or weapons.png")
    p_ext.add_argument("--type", choices=["hat", "weapon"], required=True)
    p_ext.add_argument("--row", type=int, required=True)
    p_ext.add_argument("-o", "--output", required=True)
    p_ext.set_defaults(func=cmd_extract)

    p_prev = sub.add_parser("preview", help="Render an animated GIF or contact sheet preview")
    p_prev.add_argument("--class", dest="hero_class", choices=HERO_CLASSES, default="warrior")
    p_prev.add_argument("--tier", type=int, default=2)
    p_prev.add_argument("--helmet", type=int, default=None, help="Row index in helmets.png")
    p_prev.add_argument("--weapon", default=None, help="Legacy row index OR custom weapon name")
    p_prev.add_argument("--format", choices=["gif", "sheet"], default="sheet")
    p_prev.add_argument("--scale", type=int, default=6)
    p_prev.add_argument("-o", "--output", required=True)
    p_prev.set_defaults(func=cmd_preview)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
