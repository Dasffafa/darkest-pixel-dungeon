#!/usr/bin/env python3
"""Tkinter GUI for doll_tool.py, styled after sprite_sheet_editor.py."""

from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
from pathlib import Path
import tkinter as tk
from tkinter import filedialog, messagebox, ttk

try:
    from PIL import Image, ImageDraw, ImageOps, ImageTk
except ImportError as exc:
    raise SystemExit("Pillow is required: python3 -m pip install Pillow") from exc

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

import doll_tool as dt  # noqa: E402


# --- Native zenity file dialog wrappers (same pattern as sprite_sheet_editor.py) ---

def _zenity_available():
    return os.environ.get("DISPLAY") and shutil.which("zenity")


def _zenity_run(args):
    if not _zenity_available():
        return None
    try:
        result = subprocess.run(["zenity", *args], capture_output=True, text=True)
    except OSError:
        return None
    if result.returncode == 0:
        return result.stdout.strip()
    if result.returncode == 1:
        return ""
    return None


def _zenity_filters(filetypes):
    return [f"--file-filter={label} | {patterns}" for label, patterns in (filetypes or [])]


def _native_open(title=None, filetypes=None, initialdir=None):
    args = ["--file-selection"]
    if title:
        args.append(f"--title={title}")
    if initialdir:
        args.append(f"--filename={initialdir}/")
    args.extend(_zenity_filters(filetypes))
    selected = _zenity_run(args)
    if selected is None:
        return filedialog.askopenfilename(title=title, filetypes=filetypes or [], initialdir=initialdir or "")
    return selected


def _native_save(title=None, defaultextension=None, initialfile=None, filetypes=None, initialdir=None):
    args = ["--file-selection", "--save", "--confirm-overwrite"]
    if title:
        args.append(f"--title={title}")
    start = os.path.join(initialdir, initialfile) if initialdir and initialfile else initialfile
    if start:
        args.append(f"--filename={start}")
    args.extend(_zenity_filters(filetypes))
    selected = _zenity_run(args)
    if selected is None:
        return filedialog.asksaveasfilename(
            title=title,
            defaultextension=defaultextension or "",
            initialfile=initialfile or "",
            filetypes=filetypes or [],
            initialdir=initialdir or "",
        )
    if selected and defaultextension and not selected.endswith(defaultextension):
        selected += defaultextension
    return selected


filedialog.askopenfilename = _native_open
filedialog.asksaveasfilename = _native_save


class DollToolGUI(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("DPD Paper-Doll Tool")
        self.geometry("1120x740")
        self.minsize(860, 560)

        # Preview state
        self.hero_class = tk.StringVar(value="warrior")
        self.tier = tk.IntVar(value=2)
        self.helmet_sel = tk.StringVar(value="None")
        self.weapon_sel = tk.StringVar(value="None")
        self.view_mode = tk.StringVar(value="anim")  # anim | sheet
        self.anim_state = tk.StringVar(value="idle")
        self.zoom = tk.IntVar(value=8)
        self.flip_h = tk.BooleanVar(value=False)
        self.playing = tk.BooleanVar(value=True)
        self.status = tk.StringVar(value="Ready")

        # Hat tab vars
        self.hat_input = tk.StringVar(value="")
        self.hat_row = tk.IntVar(value=0)
        self.hat_frame = tk.IntVar(value=0)
        self.hat_scope = tk.StringVar(value="Row + Frame")
        self.hat_dx = tk.IntVar(value=1)
        self.hat_dy = tk.IntVar(value=-1)
        self._helmet_meta = dt.load_helmet_meta()

        # Custom weapon tab vars
        self.weap_input = tk.StringVar(value="")
        self.weap_name = tk.StringVar(value="custom_weapon")
        self.weap_fw = tk.IntVar(value=24)
        self.weap_fh = tk.IntVar(value=24)
        self.weap_fps = tk.DoubleVar(value=12.0)
        self.weap_ox = tk.DoubleVar(value=2.0)
        self.weap_oy = tk.DoubleVar(value=0.0)
        self.weap_idle = tk.StringVar(value="0@1")
        self.weap_run = tk.StringVar(value="")
        self.weap_attack = tk.StringVar(value="")
        self.weap_operate = tk.StringVar(value="0@8")
        self.weap_fly = tk.StringVar(value="0@1")
        self.weap_extra = tk.StringVar(value="")
        self.weap_frame_idx = tk.IntVar(value=0)
        self.weap_fdx = tk.DoubleVar(value=0.0)
        self.weap_fdy = tk.DoubleVar(value=0.0)
        self._custom_frame_offsets = {}  # state -> [[dx, dy], ...]

        # Legacy weapon tab vars
        self.leg_input = tk.StringVar(value="")
        self.leg_row = tk.IntVar(value=0)

        self._photo = None
        self._anim_tick = 0
        self._after_id = None
        self._sheets_cache = {}

        self._build_ui()
        self._refresh_selectors()
        self._schedule_tick()

    def _build_ui(self):
        # Top toolbar
        tb = ttk.Frame(self, padding=6)
        tb.pack(fill=tk.X)

        ttk.Label(tb, text="Class").pack(side=tk.LEFT)
        cb_cls = ttk.Combobox(tb, textvariable=self.hero_class, values=dt.HERO_CLASSES, width=10, state="readonly")
        cb_cls.pack(side=tk.LEFT, padx=(4, 8))
        cb_cls.bind("<<ComboboxSelected>>", lambda _e: self.redraw())

        ttk.Label(tb, text="Tier").pack(side=tk.LEFT)
        ttk.Spinbox(tb, from_=0, to=11, width=4, textvariable=self.tier, command=self.redraw).pack(side=tk.LEFT, padx=(4, 8))

        ttk.Label(tb, text="Helmet").pack(side=tk.LEFT)
        self.cb_helmet = ttk.Combobox(tb, textvariable=self.helmet_sel, width=8, state="readonly")
        self.cb_helmet.pack(side=tk.LEFT, padx=(4, 8))
        self.cb_helmet.bind("<<ComboboxSelected>>", lambda _e: self._on_helmet_selected())

        ttk.Label(tb, text="Weapon").pack(side=tk.LEFT)
        self.cb_weapon = ttk.Combobox(tb, textvariable=self.weapon_sel, width=16, state="readonly")
        self.cb_weapon.pack(side=tk.LEFT, padx=(4, 8))
        self.cb_weapon.bind("<<ComboboxSelected>>", lambda _e: self._on_weapon_selected())

        ttk.Label(tb, text="Zoom").pack(side=tk.LEFT)
        ttk.Spinbox(tb, from_=2, to=16, width=4, textvariable=self.zoom, command=self.redraw).pack(side=tk.LEFT, padx=(4, 8))

        ttk.Checkbutton(tb, text="Flip", variable=self.flip_h, command=self.redraw).pack(side=tk.LEFT, padx=(0, 8))
        ttk.Button(tb, text="Reload Assets", command=self._reload_assets).pack(side=tk.RIGHT)

        # Main split: Canvas (left) + Controls Notebook (right)
        pane = ttk.PanedWindow(self, orient=tk.HORIZONTAL)
        pane.pack(fill=tk.BOTH, expand=True, padx=6, pady=(0, 4))

        left = ttk.Frame(pane)
        pane.add(left, weight=3)

        # Playback bar
        pb = ttk.Frame(left, padding=(0, 0, 0, 4))
        pb.pack(fill=tk.X)
        ttk.Radiobutton(pb, text="Anim", variable=self.view_mode, value="anim", command=self.redraw).pack(side=tk.LEFT)
        ttk.Radiobutton(pb, text="21F Sheet", variable=self.view_mode, value="sheet", command=self.redraw).pack(side=tk.LEFT, padx=(4, 8))

        ttk.Label(pb, text="State").pack(side=tk.LEFT)
        self.cb_state = ttk.Combobox(
            pb, textvariable=self.anim_state,
            values=[a[0] for a in dt.ANIMATIONS], width=10, state="readonly",
        )
        self.cb_state.pack(side=tk.LEFT, padx=(4, 8))
        self.cb_state.bind("<<ComboboxSelected>>", lambda _e: self._reset_anim_and_redraw())

        ttk.Checkbutton(pb, text="Play", variable=self.playing).pack(side=tk.LEFT)
        ttk.Button(pb, text="<", width=3, command=lambda: self._step_frame(-1)).pack(side=tk.LEFT, padx=2)
        ttk.Button(pb, text=">", width=3, command=lambda: self._step_frame(1)).pack(side=tk.LEFT)
        ttk.Button(pb, text="Export GIF", command=self._export_gif).pack(side=tk.RIGHT)

        # Canvas with scrollbars
        cframe = ttk.Frame(left)
        cframe.pack(fill=tk.BOTH, expand=True)
        self.canvas = tk.Canvas(cframe, background="#252525", highlightthickness=0, takefocus=1)
        self.canvas.bind("<Button-1>", lambda _e: self.canvas.focus_set())
        xbar = ttk.Scrollbar(cframe, orient=tk.HORIZONTAL, command=self.canvas.xview)
        ybar = ttk.Scrollbar(cframe, orient=tk.VERTICAL, command=self.canvas.yview)
        self.canvas.configure(xscrollcommand=xbar.set, yscrollcommand=ybar.set)
        self.canvas.grid(row=0, column=0, sticky="nsew")
        ybar.grid(row=0, column=1, sticky="ns")
        xbar.grid(row=1, column=0, sticky="ew")
        cframe.rowconfigure(0, weight=1)
        cframe.columnconfigure(0, weight=1)

        # Right Notebook
        self.nb = ttk.Notebook(pane)
        pane.add(self.nb, weight=2)

        self.nb.add(self._build_hat_tab(self.nb), text="Hat (helmets.png)")
        self.nb.add(self._build_custom_weapon_tab(self.nb), text="Custom Weapon")
        self.nb.add(self._build_legacy_weapon_tab(self.nb), text="Legacy weapons.png")

        # Status bar
        ttk.Label(self, textvariable=self.status, anchor="w", padding=(8, 3)).pack(fill=tk.X)

        self._install_arrow_key_bindings()
        self.canvas.focus_set()

    def _build_hat_tab(self, parent):
        f = ttk.Frame(parent, padding=10)
        ttk.Label(f, text="Pack 1F (12x15) or N*12x15 strip into helmets.png").pack(anchor="w", pady=(0, 8))

        row1 = ttk.Frame(f)
        row1.pack(fill=tk.X, pady=3)
        ttk.Label(row1, text="Input PNG:", width=12).pack(side=tk.LEFT)
        ttk.Entry(row1, textvariable=self.hat_input).pack(side=tk.LEFT, fill=tk.X, expand=True, padx=4)
        ttk.Button(row1, text="Browse", command=lambda: self._pick_png(self.hat_input)).pack(side=tk.LEFT)

        row2 = ttk.Frame(f)
        row2.pack(fill=tk.X, pady=3)
        ttk.Label(row2, text="Target Row:", width=12).pack(side=tk.LEFT)
        ttk.Spinbox(row2, from_=0, to=128, width=6, textvariable=self.hat_row).pack(side=tk.LEFT, padx=4)

        btns = ttk.Frame(f)
        btns.pack(fill=tk.X, pady=10)
        ttk.Button(btns, text="Pack into helmets.png", command=self._pack_hat).pack(side=tk.LEFT)
        ttk.Button(btns, text="Extract Row", command=self._extract_hat).pack(side=tk.LEFT, padx=6)
        ttk.Button(btns, text="Save 21F Guide", command=self._save_hat_guide).pack(side=tk.LEFT)

        ttk.Separator(f).pack(fill=tk.X, pady=8)
        ttk.Label(f, text="Hat Offset (helmets_meta.json, synced with game)").pack(anchor="w", pady=(0, 4))

        sc_row = ttk.Frame(f)
        sc_row.pack(fill=tk.X, pady=3)
        ttk.Label(sc_row, text="Scope:", width=12).pack(side=tk.LEFT)
        cb_sc = ttk.Combobox(
            sc_row, textvariable=self.hat_scope,
            values=["Default (All)", "Default + Frame", "Row (All Frames)", "Row + Frame"],
            width=16, state="readonly",
        )
        cb_sc.pack(side=tk.LEFT, padx=4)
        cb_sc.bind("<<ComboboxSelected>>", lambda _e: self._load_hat_offset())

        hf_row = ttk.Frame(f)
        hf_row.pack(fill=tk.X, pady=3)
        ttk.Label(hf_row, text="Frame (0-20):", width=12).pack(side=tk.LEFT)
        ttk.Spinbox(hf_row, from_=0, to=dt.NUM_FRAMES - 1, width=5, textvariable=self.hat_frame,
                    command=self._load_hat_offset).pack(side=tk.LEFT, padx=4)
        ttk.Button(hf_row, text="Sync Frame", command=self._sync_hat_frame_from_anim).pack(side=tk.LEFT, padx=4)

        hd_row = ttk.Frame(f)
        hd_row.pack(fill=tk.X, pady=3)
        ttk.Label(hd_row, text="dx / dy:", width=12).pack(side=tk.LEFT)
        ttk.Spinbox(hd_row, from_=-32, to=32, width=5, textvariable=self.hat_dx,
                    command=self._apply_hat_offset).pack(side=tk.LEFT, padx=4)
        ttk.Spinbox(hd_row, from_=-32, to=32, width=5, textvariable=self.hat_dy,
                    command=self._apply_hat_offset).pack(side=tk.LEFT, padx=2)
        ttk.Button(hd_row, text="Apply", command=self._apply_hat_offset).pack(side=tk.LEFT, padx=6)

        nudge_row = ttk.Frame(f)
        nudge_row.pack(fill=tk.X, pady=4)
        ttk.Button(nudge_row, text="←", width=3, command=lambda: self._nudge_hat(-1, 0)).pack(side=tk.LEFT)
        ttk.Button(nudge_row, text="↑", width=3, command=lambda: self._nudge_hat(0, -1)).pack(side=tk.LEFT, padx=2)
        ttk.Button(nudge_row, text="↓", width=3, command=lambda: self._nudge_hat(0, 1)).pack(side=tk.LEFT, padx=2)
        ttk.Button(nudge_row, text="→", width=3, command=lambda: self._nudge_hat(1, 0)).pack(side=tk.LEFT)
        ttk.Button(nudge_row, text="Save Meta", command=self._save_helmets_meta).pack(side=tk.RIGHT)
        ttk.Button(nudge_row, text="Discard", command=self._reload_assets).pack(side=tk.RIGHT, padx=4)
        return f

    def _build_custom_weapon_tab(self, parent):
        f = ttk.Frame(parent, padding=10)
        ttk.Label(f, text="Arbitrary size + per-state frames/FPS (.png + .json)").pack(anchor="w", pady=(0, 6))

        def add_row(label, var, browse=False):
            r = ttk.Frame(f)
            r.pack(fill=tk.X, pady=2)
            ttk.Label(r, text=label, width=14).pack(side=tk.LEFT)
            ttk.Entry(r, textvariable=var).pack(side=tk.LEFT, fill=tk.X, expand=True, padx=4)
            if browse:
                ttk.Button(r, text="Browse", command=lambda: self._pick_png(var)).pack(side=tk.LEFT)

        add_row("Name:", self.weap_name)
        add_row("Input PNG:", self.weap_input, browse=True)

        dims = ttk.Frame(f)
        dims.pack(fill=tk.X, pady=3)
        ttk.Label(dims, text="Frame W/H:", width=14).pack(side=tk.LEFT)
        ttk.Spinbox(dims, from_=1, to=256, width=5, textvariable=self.weap_fw).pack(side=tk.LEFT, padx=(4, 2))
        ttk.Spinbox(dims, from_=1, to=256, width=5, textvariable=self.weap_fh).pack(side=tk.LEFT, padx=2)
        ttk.Label(dims, text="FPS:").pack(side=tk.LEFT, padx=(8, 2))
        ttk.Spinbox(dims, from_=1, to=60, width=5, textvariable=self.weap_fps).pack(side=tk.LEFT)

        offs = ttk.Frame(f)
        offs.pack(fill=tk.X, pady=3)
        ttk.Label(offs, text="Offset X/Y:", width=14).pack(side=tk.LEFT)
        ttk.Spinbox(offs, from_=-64, to=64, increment=1, width=6, textvariable=self.weap_ox).pack(side=tk.LEFT, padx=(4, 2))
        ttk.Spinbox(offs, from_=-64, to=64, increment=1, width=6, textvariable=self.weap_oy).pack(side=tk.LEFT, padx=2)

        add_row("Idle:", self.weap_idle)
        add_row("Run:", self.weap_run)
        add_row("Attack:", self.weap_attack)
        add_row("Operate:", self.weap_operate)
        add_row("Fly:", self.weap_fly)
        add_row("Extra (; sep):", self.weap_extra)

        btns = ttk.Frame(f)
        btns.pack(fill=tk.X, pady=8)
        ttk.Button(btns, text="Save Custom Weapon", command=self._save_custom_weapon).pack(side=tk.LEFT)
        ttk.Button(btns, text="Save Guide", command=self._save_weapon_guide).pack(side=tk.LEFT, padx=6)

        ttk.Separator(f).pack(fill=tk.X, pady=6)
        ttk.Label(f, text="Per-Frame Weapon Offset (uses current Anim State)").pack(anchor="w", pady=(0, 4))

        wf_row = ttk.Frame(f)
        wf_row.pack(fill=tk.X, pady=2)
        ttk.Label(wf_row, text="Anim Frame:", width=14).pack(side=tk.LEFT)
        ttk.Spinbox(wf_row, from_=0, to=64, width=5, textvariable=self.weap_frame_idx,
                    command=self._load_weap_frame_offset).pack(side=tk.LEFT, padx=4)
        ttk.Label(wf_row, text="dx/dy:").pack(side=tk.LEFT, padx=(6, 2))
        ttk.Spinbox(wf_row, from_=-64, to=64, increment=1, width=5, textvariable=self.weap_fdx,
                    command=self._apply_weap_frame_offset).pack(side=tk.LEFT, padx=2)
        ttk.Spinbox(wf_row, from_=-64, to=64, increment=1, width=5, textvariable=self.weap_fdy,
                    command=self._apply_weap_frame_offset).pack(side=tk.LEFT, padx=2)

        wnudge = ttk.Frame(f)
        wnudge.pack(fill=tk.X, pady=4)
        ttk.Button(wnudge, text="←", width=3, command=lambda: self._nudge_custom_weapon(-1, 0)).pack(side=tk.LEFT)
        ttk.Button(wnudge, text="↑", width=3, command=lambda: self._nudge_custom_weapon(0, -1)).pack(side=tk.LEFT, padx=2)
        ttk.Button(wnudge, text="↓", width=3, command=lambda: self._nudge_custom_weapon(0, 1)).pack(side=tk.LEFT, padx=2)
        ttk.Button(wnudge, text="→", width=3, command=lambda: self._nudge_custom_weapon(1, 0)).pack(side=tk.LEFT)
        ttk.Button(wnudge, text="Apply", command=self._apply_weap_frame_offset).pack(side=tk.LEFT, padx=6)
        ttk.Button(wnudge, text="Clear State Offsets", command=self._clear_weap_state_offsets).pack(side=tk.RIGHT)
        return f

    def _build_legacy_weapon_tab(self, parent):
        f = ttk.Frame(parent, padding=10)
        ttk.Label(f, text="Legacy 20x19 row in weapons.png (5 cols max)").pack(anchor="w", pady=(0, 8))

        row1 = ttk.Frame(f)
        row1.pack(fill=tk.X, pady=3)
        ttk.Label(row1, text="Input PNG:", width=12).pack(side=tk.LEFT)
        ttk.Entry(row1, textvariable=self.leg_input).pack(side=tk.LEFT, fill=tk.X, expand=True, padx=4)
        ttk.Button(row1, text="Browse", command=lambda: self._pick_png(self.leg_input)).pack(side=tk.LEFT)

        row2 = ttk.Frame(f)
        row2.pack(fill=tk.X, pady=3)
        ttk.Label(row2, text="Target Row:", width=12).pack(side=tk.LEFT)
        ttk.Spinbox(row2, from_=0, to=128, width=6, textvariable=self.leg_row).pack(side=tk.LEFT, padx=4)

        btns = ttk.Frame(f)
        btns.pack(fill=tk.X, pady=10)
        ttk.Button(btns, text="Pack into weapons.png", command=self._pack_legacy_weapon).pack(side=tk.LEFT)
        ttk.Button(btns, text="Extract Row", command=self._extract_legacy_weapon).pack(side=tk.LEFT, padx=6)
        return f

    # --- Asset loading & selectors ---

    def _get_sheet(self, name: str):
        if name not in self._sheets_cache:
            self._sheets_cache[name] = dt.load_sheet(name)
        return self._sheets_cache[name]

    def _reload_assets(self):
        self._sheets_cache.clear()
        self._helmet_meta = dt.load_helmet_meta()
        self._refresh_selectors()
        self._load_hat_offset()
        self.redraw()
        self.status.set("Reloaded sprite sheets and helmets_meta.json")

    def _on_helmet_selected(self):
        if self.helmet_sel.get() != "None":
            self.hat_row.set(int(self.helmet_sel.get()))
        self._load_hat_offset()
        self.redraw()

    def _refresh_selectors(self):
        helmets = self._get_sheet("helmets.png")
        h_rows = helmets.height // dt.FRAME_H
        self.cb_helmet["values"] = ["None"] + [str(i) for i in range(h_rows)]

        weapons = self._get_sheet("weapons.png")
        w_rows = weapons.height // dt.WEAP_H
        custom_names = []
        if dt.CUSTOM_WEAP_DIR.exists():
            custom_names = sorted(p.stem for p in dt.CUSTOM_WEAP_DIR.glob("*.json"))
        self.cb_weapon["values"] = ["None"] + [f"custom:{n}" for n in custom_names] + [f"row:{i}" for i in range(w_rows)]
        if self.helmet_sel.get() == "None" and h_rows > 0:
            self.helmet_sel.set("0")
        if self.weapon_sel.get() == "None" and w_rows > 6:
            self.weapon_sel.set("row:6")
        self._load_hat_offset()
        self._update_state_choices()

    def _on_weapon_selected(self):
        sel = self.weapon_sel.get()
        if sel.startswith("custom:"):
            name = sel.split(":", 1)[1]
            self._load_custom_into_form(name)
        self._update_state_choices()
        self._reset_anim_and_redraw()

    def _update_state_choices(self):
        base = [a[0] for a in dt.ANIMATIONS]
        sel = self.weapon_sel.get()
        if sel.startswith("custom:"):
            name = sel.split(":", 1)[1]
            try:
                _, spec = dt.load_custom_weapon(name)
                for k in spec.get("animations", {}).keys():
                    if k not in base:
                        base.append(k)
            except Exception:
                pass
        self.cb_state["values"] = base
        if self.anim_state.get() not in base:
            self.anim_state.set("idle")

    def _load_custom_into_form(self, name: str):
        try:
            _, spec = dt.load_custom_weapon(name)
        except Exception:
            return
        self.weap_name.set(name)
        self.weap_input.set(str(dt.CUSTOM_WEAP_DIR / f"{name}.png"))
        self.weap_fw.set(int(spec.get("frameWidth", 20)))
        self.weap_fh.set(int(spec.get("frameHeight", 19)))
        self.weap_ox.set(float(spec.get("offsetX", 0)))
        self.weap_oy.set(float(spec.get("offsetY", 0)))
        anims = spec.get("animations", {})

        def fmt(entry):
            if not entry:
                return ""
            seq = ",".join(str(x) for x in entry.get("frames", []))
            return f"{seq}@{entry.get('fps', 10)}"

        self.weap_idle.set(fmt(anims.get("idle")))
        self.weap_run.set(fmt(anims.get("run")))
        self.weap_attack.set(fmt(anims.get("attack")))
        self.weap_operate.set(fmt(anims.get("operate")))
        self.weap_fly.set(fmt(anims.get("fly")))
        extras = []
        std = {"idle", "run", "attack", "operate", "fly"}
        self._custom_frame_offsets = {}
        for k, v in anims.items():
            if isinstance(v.get("offsets"), list):
                self._custom_frame_offsets[k] = [list(pair) for pair in v["offsets"]]
            if k not in std:
                suffix = "!loop" if v.get("looped") else ""
                extras.append(f"{k}={fmt(v)}{suffix}")
        self.weap_extra.set("; ".join(extras))
        self._load_weap_frame_offset()

    # --- File actions ---

    def _pick_png(self, var: tk.StringVar):
        path = filedialog.askopenfilename(title="Select PNG", filetypes=[("PNG images", "*.png")])
        if path:
            var.set(path)

    def _pack_hat(self):
        src = self.hat_input.get().strip()
        if not src:
            messagebox.showwarning("Missing input", "Choose an input PNG first.")
            return
        row = int(self.hat_row.get())
        try:
            ns = type("Args", (), {"input": src, "row": row, "sheet": str(dt.HERO_DIR / "helmets.png")})()
            dt.cmd_hat(ns)
            self._sheets_cache.pop("helmets.png", None)
            self._refresh_selectors()
            self.helmet_sel.set(str(row))
            self.redraw()
            self.status.set(f"Packed hat into helmets.png row {row}")
        except Exception as exc:
            messagebox.showerror("Pack hat failed", str(exc))

    def _extract_hat(self):
        row = int(self.hat_row.get())
        out = filedialog.asksaveasfilename(title="Save extracted hat row", defaultextension=".png", initialfile=f"hat_row_{row}.png")
        if not out:
            return
        ns = type("Args", (), {"type": "hat", "row": row, "output": out})()
        dt.cmd_extract(ns)
        self.status.set(f"Extracted hat row {row} -> {out}")

    def _save_hat_guide(self):
        out = filedialog.asksaveasfilename(title="Save 21F Hat Guide", defaultextension=".png", initialfile="hat_guide.png")
        if not out:
            return
        ns = type("Args", (), {"type": "hat", "hero_class": self.hero_class.get(), "tier": int(self.tier.get()),
                               "frame_width": None, "frame_height": None, "output": out})()
        dt.cmd_guide(ns)
        self.status.set(f"Saved hat guide -> {out}")

    def _save_custom_weapon(self):
        src = self.weap_input.get().strip()
        name = self.weap_name.get().strip()
        if not src or not name:
            messagebox.showwarning("Missing fields", "Provide both weapon Name and Input PNG.")
            return
        extras = [t.strip() for t in self.weap_extra.get().split(";") if t.strip()]
        ns = type("Args", (), {
            "input": src,
            "name": name,
            "row": None,
            "sheet": str(dt.HERO_DIR / "weapons.png"),
            "frame_width": int(self.weap_fw.get()),
            "frame_height": int(self.weap_fh.get()),
            "fps": float(self.weap_fps.get()),
            "offset_x": float(self.weap_ox.get()),
            "offset_y": float(self.weap_oy.get()),
            "idle": self.weap_idle.get().strip() or None,
            "run": self.weap_run.get().strip() or None,
            "attack": self.weap_attack.get().strip() or None,
            "operate": self.weap_operate.get().strip() or None,
            "fly": self.weap_fly.get().strip() or None,
            "extra": extras,
        })()
        try:
            dt.cmd_weapon(ns)
            if self._custom_frame_offsets:
                jpath = dt.CUSTOM_WEAP_DIR / f"{name}.json"
                spec = json.loads(jpath.read_text(encoding="utf-8"))
                for st, offs in self._custom_frame_offsets.items():
                    if st in spec.get("animations", {}) and offs:
                        spec["animations"][st]["offsets"] = offs
                jpath.write_text(json.dumps(spec, indent=2) + "\n", encoding="utf-8")
            self._refresh_selectors()
            self.weapon_sel.set(f"custom:{name}")
            self._update_state_choices()
            self.redraw()
            self.status.set(f"Saved custom weapon '{name}' (.png + .json)")
        except Exception as exc:
            messagebox.showerror("Save weapon failed", str(exc))

    def _save_weapon_guide(self):
        out = filedialog.asksaveasfilename(title="Save Weapon Guide", defaultextension=".png", initialfile="weapon_guide.png")
        if not out:
            return
        ns = type("Args", (), {
            "type": "weapon",
            "hero_class": self.hero_class.get(),
            "tier": int(self.tier.get()),
            "frame_width": int(self.weap_fw.get()),
            "frame_height": int(self.weap_fh.get()),
            "output": out,
        })()
        dt.cmd_guide(ns)
        self.status.set(f"Saved weapon guide ({self.weap_fw.get()}x{self.weap_fh.get()}) -> {out}")

    def _pack_legacy_weapon(self):
        src = self.leg_input.get().strip()
        if not src:
            messagebox.showwarning("Missing input", "Choose an input PNG first.")
            return
        row = int(self.leg_row.get())
        try:
            ns = type("Args", (), {"input": src, "row": row, "name": None, "sheet": str(dt.HERO_DIR / "weapons.png")})()
            dt.cmd_weapon(ns)
            self._sheets_cache.pop("weapons.png", None)
            self._refresh_selectors()
            self.weapon_sel.set(f"row:{row}")
            self.redraw()
            self.status.set(f"Packed legacy weapon into weapons.png row {row}")
        except Exception as exc:
            messagebox.showerror("Pack legacy weapon failed", str(exc))

    def _extract_legacy_weapon(self):
        row = int(self.leg_row.get())
        out = filedialog.asksaveasfilename(title="Save extracted weapon row", defaultextension=".png", initialfile=f"weapon_row_{row}.png")
        if not out:
            return
        ns = type("Args", (), {"type": "weapon", "row": row, "output": out})()
        dt.cmd_extract(ns)
        self.status.set(f"Extracted weapon row {row} -> {out}")

    def _export_gif(self):
        out = filedialog.asksaveasfilename(title="Export GIF Preview", defaultextension=".gif", initialfile="preview.gif")
        if not out:
            return
        h_val = None if self.helmet_sel.get() == "None" else int(self.helmet_sel.get())
        wsel = self.weapon_sel.get()
        w_val = None
        if wsel.startswith("row:"):
            w_val = wsel.split(":", 1)[1]
        elif wsel.startswith("custom:"):
            w_val = wsel.split(":", 1)[1]
        ns = type("Args", (), {
            "hero_class": self.hero_class.get(),
            "tier": int(self.tier.get()),
            "helmet": h_val,
            "weapon": w_val,
            "format": "gif",
            "scale": int(self.zoom.get()),
            "output": out,
        })()
        dt.cmd_preview(ns)
        self.status.set(f"Exported GIF -> {out}")

    # --- Per-frame hat & custom weapon offset adjustments ---

    def _current_body_frame(self) -> int:
        state = self.anim_state.get()
        anim_tuple = next((a for a in dt.ANIMATIONS if a[0] == state), dt.ANIMATIONS[0])
        b_frames = anim_tuple[1]
        return b_frames[self._anim_tick % len(b_frames)]

    def _sync_hat_frame_from_anim(self):
        self.hat_frame.set(self._current_body_frame())
        self._load_hat_offset()
        self.status.set(f"Hat frame synced to {self.hat_frame.get()}")

    def _active_hat_row(self) -> int:
        return int(self.hat_row.get() if self.helmet_sel.get() == "None" else self.helmet_sel.get())

    def _load_hat_offset(self):
        row = self._active_hat_row()
        col = int(self.hat_frame.get())
        scope = self.hat_scope.get()
        meta = self._helmet_meta
        if scope == "Default (All)":
            dx, dy = meta.get("defaultOffset", [1, -1])
        elif scope == "Default + Frame":
            dx, dy = meta.get("defaultFrameOffsets", {}).get(str(col), meta.get("defaultOffset", [1, -1]))
        elif scope == "Row (All Frames)":
            rcfg = meta.get("rows", {}).get(str(row), {})
            dx, dy = rcfg.get("offset", meta.get("defaultOffset", [1, -1]))
        else:
            dx, dy = dt.helmet_offset(meta, row, col)
        self.hat_dx.set(int(dx))
        self.hat_dy.set(int(dy))

    def _apply_hat_offset(self):
        row = self._active_hat_row()
        col = int(self.hat_frame.get())
        dx, dy = int(self.hat_dx.get()), int(self.hat_dy.get())
        scope = self.hat_scope.get()
        meta = self._helmet_meta
        if scope == "Default (All)":
            meta["defaultOffset"] = [dx, dy]
        elif scope == "Default + Frame":
            meta.setdefault("defaultFrameOffsets", {})[str(col)] = [dx, dy]
        elif scope == "Row (All Frames)":
            meta.setdefault("rows", {}).setdefault(str(row), {})["offset"] = [dx, dy]
        else:
            meta.setdefault("rows", {}).setdefault(str(row), {}).setdefault("frameOffsets", {})[str(col)] = [dx, dy]

        if "Frame" in scope:
            self.playing.set(False)
            state = self.anim_state.get()
            anim_tuple = next((a for a in dt.ANIMATIONS if a[0] == state), dt.ANIMATIONS[0])
            if col in anim_tuple[1]:
                self._anim_tick = anim_tuple[1].index(col)
        self.redraw()
        self.status.set(f"[{scope}] row={row} frame={col} -> ({dx}, {dy}) — click 'Save Meta' to persist")

    def _nudge_hat(self, dx: int, dy: int):
        self.hat_dx.set(int(self.hat_dx.get()) + dx)
        self.hat_dy.set(int(self.hat_dy.get()) + dy)
        self._apply_hat_offset()

    def _install_arrow_key_bindings(self):
        """Intercept arrow keys before ttk.Notebook/Button/Combobox hijack them for focus/tab switching."""
        def attach(widget):
            if not isinstance(widget, (tk.Entry, ttk.Entry, ttk.Spinbox)):
                tags = widget.bindtags()
                if "NudgeKeys" not in tags:
                    widget.bindtags(("NudgeKeys", *tags))
            for child in widget.winfo_children():
                attach(child)

        attach(self)
        self.bind_class("NudgeKeys", "<Left>", lambda _e: self._on_arrow_nudge(-1, 0))
        self.bind_class("NudgeKeys", "<Right>", lambda _e: self._on_arrow_nudge(1, 0))
        self.bind_class("NudgeKeys", "<Up>", lambda _e: self._on_arrow_nudge(0, -1))
        self.bind_class("NudgeKeys", "<Down>", lambda _e: self._on_arrow_nudge(0, 1))
        self.bind_all("<Escape>", lambda _e: self.canvas.focus_set())

    def _on_arrow_nudge(self, dx: int, dy: int):
        tab_idx = self.nb.index(self.nb.select())
        if tab_idx == 0:
            self._nudge_hat(dx, dy)
        elif tab_idx == 1:
            self._nudge_custom_weapon(dx, dy)
        return "break"

    def _save_helmets_meta(self):
        dt.save_helmet_meta(self._helmet_meta)
        self.status.set(f"Saved {dt.HELMET_META_PATH}")

    def _current_custom_anim(self):
        wsel = self.weapon_sel.get()
        if not wsel.startswith("custom:"):
            return None, None, None
        name = wsel.split(":", 1)[1]
        try:
            _, spec = dt.load_custom_weapon(name)
        except Exception:
            return name, None, None
        state = self.anim_state.get()
        anims = spec.get("animations", {})
        anim = anims.get(state) or anims.get("idle")
        return name, state, anim

    def _load_weap_frame_offset(self):
        _, state, anim = self._current_custom_anim()
        if not anim:
            return
        wf_list = anim.get("frames") or [0]
        idx = max(0, min(int(self.weap_frame_idx.get()), len(wf_list) - 1))
        self.weap_frame_idx.set(idx)
        offs = self._custom_frame_offsets.get(state)
        if offs and idx < len(offs):
            self.weap_fdx.set(float(offs[idx][0]))
            self.weap_fdy.set(float(offs[idx][1]))
        else:
            self.weap_fdx.set(float(anim.get("offsetX", self.weap_ox.get())))
            self.weap_fdy.set(float(anim.get("offsetY", self.weap_oy.get())))

    def _ensure_state_offsets(self, state: str, anim: dict):
        wf_list = anim.get("frames") or [0]
        def_ox = float(anim.get("offsetX", self.weap_ox.get()))
        def_oy = float(anim.get("offsetY", self.weap_oy.get()))
        offs = self._custom_frame_offsets.get(state)
        if not offs or len(offs) != len(wf_list):
            existing = anim.get("offsets") or []
            offs = [
                [float(existing[i][0]), float(existing[i][1])] if i < len(existing) else [def_ox, def_oy]
                for i in range(len(wf_list))
            ]
            self._custom_frame_offsets[state] = offs
        return offs

    def _apply_weap_frame_offset(self):
        _, state, anim = self._current_custom_anim()
        if not anim:
            return
        offs = self._ensure_state_offsets(state, anim)
        idx = max(0, min(int(self.weap_frame_idx.get()), len(offs) - 1))
        offs[idx] = [float(self.weap_fdx.get()), float(self.weap_fdy.get())]
        self.playing.set(False)
        self._anim_tick = idx
        self.redraw()
        self.status.set(f"Set {state} frame {idx} offset = ({offs[idx][0]}, {offs[idx][1]}) — click 'Save Custom Weapon' to persist")

    def _nudge_custom_weapon(self, dx: int, dy: int):
        _, state, anim = self._current_custom_anim()
        if not anim:
            messagebox.showwarning("No custom weapon", "Select or save a custom:* weapon first.")
            return
        offs = self._ensure_state_offsets(state, anim)
        idx = max(0, min(int(self.weap_frame_idx.get()), len(offs) - 1))
        offs[idx][0] += dx
        offs[idx][1] += dy
        self.weap_fdx.set(offs[idx][0])
        self.weap_fdy.set(offs[idx][1])
        self.playing.set(False)
        self._anim_tick = idx
        self.redraw()
        self.status.set(f"Nudged {state} frame {idx} -> ({offs[idx][0]}, {offs[idx][1]}) — click 'Save Custom Weapon' to persist")

    def _clear_weap_state_offsets(self):
        state = self.anim_state.get()
        if state in self._custom_frame_offsets:
            del self._custom_frame_offsets[state]
        self._load_weap_frame_offset()
        self.redraw()
        self.status.set(f"Cleared per-frame offsets for '{state}'")

    # --- Rendering & animation loop ---

    def _reset_anim_and_redraw(self):
        self._anim_tick = 0
        self._load_weap_frame_offset()
        self.redraw()

    def _step_frame(self, delta: int):
        self.playing.set(False)
        self._anim_tick += delta
        self.hat_frame.set(self._current_body_frame())
        self._load_hat_offset()
        _, _, anim = self._current_custom_anim()
        if anim and anim.get("frames"):
            self.weap_frame_idx.set(self._anim_tick % len(anim["frames"]))
            self._load_weap_frame_offset()
        self.redraw()

    def _schedule_tick(self):
        if self._after_id is not None:
            self.after_cancel(self._after_id)
        if self.playing.get() and self.view_mode.get() == "anim":
            self._anim_tick += 1
            self.redraw()
        self._after_id = self.after(120, self._schedule_tick)

    def redraw(self):
        hc = dt.HERO_CLASSES.index(self.hero_class.get())
        tier = int(self.tier.get())
        scale = max(2, int(self.zoom.get()))
        body = self._get_sheet("body.png")
        head = self._get_sheet("head.png")
        armor = self._get_sheet("armor.png")

        h_row = None if self.helmet_sel.get() == "None" else int(self.helmet_sel.get())
        helmets = self._get_sheet("helmets.png") if h_row is not None else None

        wsel = self.weapon_sel.get()
        w_row = None
        weapons = None
        custom_weap = None
        if wsel.startswith("row:"):
            w_row = int(wsel.split(":", 1)[1])
            weapons = self._get_sheet("weapons.png")
        elif wsel.startswith("custom:"):
            try:
                custom_weap = dt.load_custom_weapon(wsel.split(":", 1)[1])
            except Exception:
                custom_weap = None

        pad = 14 if custom_weap is None else max(14, custom_weap[1].get("frameWidth", 20) // 2 + 4)

        if self.view_mode.get() == "sheet":
            cw, ch = dt.FRAME_W + pad * 2, dt.FRAME_H + pad * 2
            cols, rows = 7, 3
            img = Image.new("RGBA", (cols * cw * scale, rows * ch * scale), (37, 37, 37, 255))
            draw = ImageDraw.Draw(img)
            for idx in range(dt.NUM_FRAMES):
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

                comp = dt.compose_hero_frame(
                    body, head, armor, helmets, weapons,
                    hc, tier, h_row, w_row, idx, w_col, w_dx, 0, pad=pad,
                    helmet_meta=self._helmet_meta,
                )
                if custom_weap is not None and idx not in (8, 9, 10, 11, 12, 19, 20):
                    wsheet, wspec = custom_weap
                    anim = wspec.get("animations", {}).get("idle")
                    if anim and anim.get("frames"):
                        dt.compose_custom_weapon(
                            comp, wsheet,
                            wspec.get("frameWidth", 20), wspec.get("frameHeight", 19),
                            anim["frames"][0],
                            anim.get("offsetX", wspec.get("offsetX", 0)),
                            anim.get("offsetY", wspec.get("offsetY", 0)),
                            pad,
                        )
                if self.flip_h.get():
                    comp = ImageOps.mirror(comp)
                cell = comp.resize((cw * scale, ch * scale), Image.Resampling.NEAREST)
                gx, gy = (idx % cols) * cw * scale, (idx // cols) * ch * scale
                img.alpha_composite(cell, (gx, gy))
                draw.text((gx + 4, gy + 4), str(idx), fill=(210, 210, 210, 255))
        else:
            state = self.anim_state.get()
            anim_tuple = next((a for a in dt.ANIMATIONS if a[0] == state), dt.ANIMATIONS[0])
            _, b_frames, _, w_frames, w_dx_list, w_dy_list = anim_tuple
            bi = self._anim_tick % len(b_frames)
            bcol = b_frames[bi]
            wcol = w_frames[bi] if w_frames else None
            wdx = w_dx_list[bi] if w_dx_list else 0
            wdy = w_dy_list[bi] if w_dy_list else 0

            comp = dt.compose_hero_frame(
                body, head, armor, helmets, weapons,
                hc, tier, h_row, w_row, bcol, wcol, wdx, wdy, pad=pad,
                helmet_meta=self._helmet_meta,
            )
            if custom_weap is not None and state not in ("read", "die"):
                wsheet, wspec = custom_weap
                anims = wspec.get("animations", {})
                anim = anims.get(state) or anims.get("idle")
                if anim and anim.get("frames"):
                    wf_list = anim["frames"]
                    wi = self._anim_tick % len(wf_list)
                    live_offs = self._custom_frame_offsets.get(state) or anim.get("offsets") or []
                    fo = live_offs[wi] if wi < len(live_offs) else None
                    ox = fo[0] if fo else anim.get("offsetX", wspec.get("offsetX", 0))
                    oy = fo[1] if fo else anim.get("offsetY", wspec.get("offsetY", 0))
                    dt.compose_custom_weapon(
                        comp, wsheet,
                        wspec.get("frameWidth", 20), wspec.get("frameHeight", 19),
                        wf_list[wi], ox, oy, pad,
                    )
            if self.flip_h.get():
                comp = ImageOps.mirror(comp)
            img = comp.resize((comp.width * scale, comp.height * scale), Image.Resampling.NEAREST)

        self._photo = ImageTk.PhotoImage(img)
        self.canvas.delete("all")
        self.canvas.create_image(12, 12, image=self._photo, anchor="nw")
        self.canvas.configure(scrollregion=(0, 0, img.width + 24, img.height + 24))


if __name__ == "__main__":
    app = DollToolGUI()
    app.mainloop()
