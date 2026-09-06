#!/usr/bin/env python3
"""Small GUI utility for inspecting and editing sprite sheets."""

from pathlib import Path
import tkinter as tk
from tkinter import filedialog, messagebox, simpledialog, ttk

try:
    from PIL import Image, ImageTk
except ImportError as exc:
    raise SystemExit("Pillow is required: python3 -m pip install Pillow") from exc


def find_alpha_blobs(image, threshold, min_area):
    """Return bounding boxes for 8-connected alpha regions in scan order."""
    alpha = image.getchannel("A")
    pixels = alpha.load()
    width, height = image.size
    visited = bytearray(width * height)
    blobs = []
    for sy in range(height):
        for sx in range(width):
            start = sy * width + sx
            if visited[start] or pixels[sx, sy] <= threshold:
                continue
            stack = [(sx, sy)]
            visited[start] = 1
            min_x = max_x = sx
            min_y = max_y = sy
            count = 0
            while stack:
                x, y = stack.pop()
                count += 1
                min_x, max_x = min(min_x, x), max(max_x, x)
                min_y, max_y = min(min_y, y), max(max_y, y)
                for ny in range(max(0, y - 1), min(height, y + 2)):
                    for nx in range(max(0, x - 1), min(width, x + 2)):
                        offset = ny * width + nx
                        if not visited[offset] and pixels[nx, ny] > threshold:
                            visited[offset] = 1
                            stack.append((nx, ny))
            if count >= min_area:
                blobs.append((min_x, min_y, max_x + 1, max_y + 1))
    return blobs


class SpriteSheetEditor(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Sprite Sheet Editor")
        self.geometry("1000x720")
        self.minsize(720, 480)

        self.image = None
        self.image_path = None
        self.preview = None
        self.selected = set()
        self.tile_w = tk.IntVar(value=16)
        self.tile_h = tk.IntVar(value=16)
        self.zoom = tk.IntVar(value=3)
        self.status = tk.StringVar(value="Open an image to begin")
        self.drag_start = None

        self._build_ui()

    def _build_ui(self):
        toolbar = ttk.Frame(self, padding=6)
        toolbar.pack(fill=tk.X)
        ttk.Button(toolbar, text="Open", command=self.open_image).pack(side=tk.LEFT)
        ttk.Button(toolbar, text="Save Sheet", command=self.save_sheet).pack(side=tk.LEFT, padx=(4, 12))

        ttk.Label(toolbar, text="Tile width").pack(side=tk.LEFT)
        ttk.Spinbox(toolbar, from_=1, to=4096, width=6, textvariable=self.tile_w,
                    command=self.rebuild_grid).pack(side=tk.LEFT, padx=(4, 8))
        ttk.Label(toolbar, text="Tile height").pack(side=tk.LEFT)
        ttk.Spinbox(toolbar, from_=1, to=4096, width=6, textvariable=self.tile_h,
                    command=self.rebuild_grid).pack(side=tk.LEFT, padx=(4, 8))
        ttk.Button(toolbar, text="Apply Grid", command=self.rebuild_grid).pack(side=tk.LEFT)

        ttk.Label(toolbar, text="Zoom").pack(side=tk.LEFT, padx=(12, 0))
        ttk.Spinbox(toolbar, from_=1, to=16, width=4, textvariable=self.zoom,
                    command=self.redraw).pack(side=tk.LEFT, padx=4)

        ttk.Button(toolbar, text="Replace Selected", command=self.replace_selected).pack(side=tk.RIGHT)
        ttk.Button(toolbar, text="Export Selected", command=self.export_selected).pack(side=tk.RIGHT, padx=4)
        ttk.Button(toolbar, text="More Tools", command=self.open_tools).pack(side=tk.RIGHT, padx=(4, 12))

        body = ttk.Frame(self)
        body.pack(fill=tk.BOTH, expand=True)
        self.canvas = tk.Canvas(body, background="#252525", highlightthickness=0)
        xbar = ttk.Scrollbar(body, orient=tk.HORIZONTAL, command=self.canvas.xview)
        ybar = ttk.Scrollbar(body, orient=tk.VERTICAL, command=self.canvas.yview)
        self.canvas.configure(xscrollcommand=xbar.set, yscrollcommand=ybar.set)
        self.canvas.grid(row=0, column=0, sticky="nsew")
        ybar.grid(row=0, column=1, sticky="ns")
        xbar.grid(row=1, column=0, sticky="ew")
        body.rowconfigure(0, weight=1)
        body.columnconfigure(0, weight=1)

        ttk.Label(self, textvariable=self.status, anchor=tk.W, padding=(6, 4)).pack(fill=tk.X)
        self.canvas.bind("<Button-1>", self.on_press)
        self.canvas.bind("<B1-Motion>", self.on_drag)
        self.canvas.bind("<ButtonRelease-1>", self.on_release)
        self.bind("<Control-a>", self.select_all)
        self.bind("<Escape>", self.clear_selection)

    def open_image(self):
        name = filedialog.askopenfilename(filetypes=[
            ("Images", "*.png *.jpg *.jpeg *.bmp *.gif *.webp"), ("All files", "*")])
        if not name:
            return
        try:
            self.image = Image.open(name).convert("RGBA")
        except Exception as exc:
            messagebox.showerror("Open failed", str(exc))
            return
        self.image_path = Path(name)
        self.selected.clear()
        self.rebuild_grid()

    def dimensions(self):
        if self.image is None:
            return 0, 0, 0, 0
        try:
            tw, th = self.tile_w.get(), self.tile_h.get()
        except tk.TclError:
            return 0, 0, 0, 0
        if tw <= 0 or th <= 0:
            return 0, 0, 0, 0
        cols = self.image.width // tw
        rows = self.image.height // th
        return tw, th, cols, rows

    def rebuild_grid(self):
        if self.image is None:
            return
        tw, th, cols, rows = self.dimensions()
        if cols == 0 or rows == 0:
            messagebox.showwarning("Invalid grid", "Tile size must fit inside the image.")
            return
        self.selected = {i for i in self.selected if i < cols * rows}
        remainder = self.image.width % tw or self.image.height % th
        if remainder:
            self.status.set("Warning: partial tiles at the right or bottom edge are ignored")
        self.redraw()

    def redraw(self):
        if self.image is None:
            return
        tw, th, cols, rows = self.dimensions()
        if not cols or not rows:
            return
        scale = max(1, self.zoom.get())
        shown = self.image.resize((self.image.width * scale, self.image.height * scale), Image.Resampling.NEAREST)
        self.preview = ImageTk.PhotoImage(shown)
        self.canvas.delete("all")
        self.canvas.create_image(0, 0, image=self.preview, anchor=tk.NW)

        sw, sh = tw * scale, th * scale
        for row in range(rows):
            for col in range(cols):
                index = row * cols + col
                x1, y1 = col * sw, row * sh
                color = "#ffd54a" if index in self.selected else "#808080"
                width = 3 if index in self.selected else 1
                self.canvas.create_rectangle(x1, y1, x1 + sw, y1 + sh,
                                             outline=color, width=width)
                if index in self.selected:
                    self.canvas.create_text(x1 + 3, y1 + 2, text=str(index), anchor=tk.NW,
                                            fill="#ffffff", font=("TkDefaultFont", 9, "bold"))
        self.canvas.configure(scrollregion=(0, 0, shown.width, shown.height))
        self.update_status()

    def event_tile(self, event):
        tw, th, cols, rows = self.dimensions()
        if not cols or not rows:
            return None
        scale = max(1, self.zoom.get())
        col = int(self.canvas.canvasx(event.x)) // (tw * scale)
        row = int(self.canvas.canvasy(event.y)) // (th * scale)
        return row * cols + col if 0 <= col < cols and 0 <= row < rows else None

    def on_press(self, event):
        index = self.event_tile(event)
        if index is None:
            return
        self.drag_start = index
        if event.state & 0x0004:
            self.selected.symmetric_difference_update({index})
        else:
            self.selected = {index}
        self.redraw()

    def on_drag(self, event):
        index = self.event_tile(event)
        if index is None or self.drag_start is None:
            return
        _, _, cols, _ = self.dimensions()
        r1, c1 = divmod(self.drag_start, cols)
        r2, c2 = divmod(index, cols)
        area = {r * cols + c for r in range(min(r1, r2), max(r1, r2) + 1)
                for c in range(min(c1, c2), max(c1, c2) + 1)}
        self.selected = area
        self.redraw()

    def on_release(self, _event):
        self.drag_start = None

    def select_all(self, _event=None):
        _, _, cols, rows = self.dimensions()
        self.selected = set(range(cols * rows))
        self.redraw()
        return "break"

    def clear_selection(self, _event=None):
        self.selected.clear()
        self.redraw()
        return "break"

    def tile_box(self, index):
        tw, th, cols, _ = self.dimensions()
        row, col = divmod(index, cols)
        return col * tw, row * th, (col + 1) * tw, (row + 1) * th

    def export_selected(self):
        if self.image is None or not self.selected:
            messagebox.showinfo("Nothing selected", "Select one or more tiles first.")
            return
        directory = filedialog.askdirectory(title="Export selected tiles")
        if not directory:
            return
        stem = self.image_path.stem if self.image_path else "tile"
        for index in sorted(self.selected):
            self.image.crop(self.tile_box(index)).save(Path(directory) / f"{stem}_{index}.png")
        self.status.set(f"Exported {len(self.selected)} tile(s) to {directory}")

    def replace_selected(self):
        if self.image is None or not self.selected:
            messagebox.showinfo("Nothing selected", "Select one or more tiles first.")
            return
        name = filedialog.askopenfilename(filetypes=[("Images", "*.png *.jpg *.jpeg *.bmp *.gif *.webp")])
        if not name:
            return
        try:
            replacement = Image.open(name).convert("RGBA")
            tw, th, _, _ = self.dimensions()
            replacement = replacement.resize((tw, th), Image.Resampling.NEAREST)
            for index in self.selected:
                self.image.alpha_composite(replacement, self.tile_box(index)[:2])
        except Exception as exc:
            messagebox.showerror("Replace failed", str(exc))
            return
        self.redraw()
        self.status.set(f"Replaced {len(self.selected)} tile(s); use Save Sheet to write the result")

    def save_sheet(self):
        if self.image is None:
            return
        initial = f"{self.image_path.stem}_edited.png" if self.image_path else "sheet_edited.png"
        name = filedialog.asksaveasfilename(defaultextension=".png", initialfile=initial,
                                            filetypes=[("PNG image", "*.png"), ("All files", "*")])
        if not name:
            return
        try:
            self.image.save(name)
            self.status.set(f"Saved {name}")
        except Exception as exc:
            messagebox.showerror("Save failed", str(exc))

    def open_tools(self):
        dialog = tk.Toplevel(self)
        dialog.title("Sprite Tools")
        dialog.resizable(False, False)
        dialog.transient(self)
        frame = ttk.Frame(dialog, padding=14)
        frame.pack(fill=tk.BOTH, expand=True)
        ttk.Label(frame, text="Content-aware sprite operations").pack(anchor=tk.W, pady=(0, 10))
        ttk.Button(frame, text="Slice Current Image by Alpha Regions",
                   command=lambda: (dialog.destroy(), self.slice_alpha_regions())).pack(fill=tk.X, pady=3)
        ttk.Button(frame, text="Convert GIF Frames to Sprite Sheet",
                   command=lambda: (dialog.destroy(), self.gif_to_sheet())).pack(fill=tk.X, pady=3)
        ttk.Button(frame, text="Strip Transparent Edges from PNG Folder",
                   command=lambda: (dialog.destroy(), self.strip_png_folder())).pack(fill=tk.X, pady=3)
        ttk.Button(frame, text="Close", command=dialog.destroy).pack(fill=tk.X, pady=(12, 0))

    def slice_alpha_regions(self):
        if self.image is None:
            messagebox.showinfo("No image", "Open an image first.")
            return
        threshold = simpledialog.askinteger("Alpha threshold", "Pixels above this alpha are content:",
                                            initialvalue=16, minvalue=0, maxvalue=254, parent=self)
        if threshold is None:
            return
        min_area = simpledialog.askinteger("Minimum area", "Ignore regions with fewer pixels than:",
                                           initialvalue=4, minvalue=1, parent=self)
        if min_area is None:
            return
        pad = simpledialog.askinteger("Padding", "Transparent padding around each result:",
                                      initialvalue=1, minvalue=0, parent=self)
        if pad is None:
            return
        prefix = simpledialog.askstring("Filename prefix", "Output filename prefix:",
                                        initialvalue="item", parent=self)
        if prefix is None:
            return
        directory = filedialog.askdirectory(title="Export alpha regions")
        if not directory:
            return
        try:
            blobs = find_alpha_blobs(self.image, threshold, min_area)
            for index, (left, top, right, bottom) in enumerate(blobs):
                box = (max(0, left - pad), max(0, top - pad),
                       min(self.image.width, right + pad), min(self.image.height, bottom + pad))
                self.image.crop(box).save(Path(directory) / f"{prefix}_{left}_{top}.png")
        except Exception as exc:
            messagebox.showerror("Slice failed", str(exc))
            return
        self.status.set(f"Exported {len(blobs)} alpha region(s) to {directory}")

    def gif_to_sheet(self):
        name = filedialog.askopenfilename(title="Open animated GIF", filetypes=[("GIF image", "*.gif")])
        if not name:
            return
        cols = simpledialog.askinteger("Columns", "Frames per row (0 means one row):",
                                       initialvalue=0, minvalue=0, parent=self)
        if cols is None:
            return
        pad = simpledialog.askinteger("Frame spacing", "Pixels between frames:",
                                      initialvalue=0, minvalue=0, parent=self)
        if pad is None:
            return
        transparent = messagebox.askyesno("Background transparency",
                                          "Make pixels matching the first frame's top-left color transparent?")
        output = filedialog.asksaveasfilename(defaultextension=".png",
                                              initialfile=f"{Path(name).stem}.sheet.png",
                                              filetypes=[("PNG image", "*.png")])
        if not output:
            return
        try:
            source = Image.open(name)
            frames = []
            frame_index = 0
            while True:
                try:
                    source.seek(frame_index)
                except EOFError:
                    break
                frames.append(source.convert("RGBA"))
                frame_index += 1
            if not frames:
                raise ValueError("The GIF contains no readable frames")
            if transparent:
                background = frames[0].getpixel((0, 0))[:3]
                processed = []
                for frame in frames:
                    data = [(r, g, b, 0 if (r, g, b) == background else a)
                            for r, g, b, a in frame.getdata()]
                    frame = frame.copy()
                    frame.putdata(data)
                    processed.append(frame)
                frames = processed
            columns = min(cols, len(frames)) if cols else len(frames)
            rows = (len(frames) + columns - 1) // columns
            fw, fh = frames[0].size
            sheet = Image.new("RGBA", (columns * fw + (columns - 1) * pad,
                                        rows * fh + (rows - 1) * pad))
            for index, frame in enumerate(frames):
                sheet.alpha_composite(frame, ((index % columns) * (fw + pad),
                                               (index // columns) * (fh + pad)))
            sheet.save(output)
            self.image = sheet
            self.image_path = Path(output)
            self.tile_w.set(fw)
            self.tile_h.set(fh)
            self.selected.clear()
            self.rebuild_grid()
            self.status.set(f"Created {len(frames)}-frame sheet: {output}")
        except Exception as exc:
            messagebox.showerror("GIF conversion failed", str(exc))

    def strip_png_folder(self):
        source_dir = filedialog.askdirectory(title="Folder containing PNG files")
        if not source_dir:
            return
        recursive = messagebox.askyesno("Subfolders", "Process PNG files in subfolders too?")
        pad = simpledialog.askinteger("Padding", "Transparent padding to retain:",
                                      initialvalue=0, minvalue=0, parent=self)
        if pad is None:
            return
        threshold = simpledialog.askinteger("Alpha threshold", "Pixels above this alpha are content:",
                                            initialvalue=16, minvalue=0, maxvalue=254, parent=self)
        if threshold is None:
            return
        in_place = messagebox.askyesno("Output mode",
                                       "Overwrite source PNG files?\nChoose No to select an output folder.")
        output_dir = None if in_place else filedialog.askdirectory(title="Output folder")
        if not in_place and not output_dir:
            return
        changed = unchanged = failed = 0
        base = Path(source_dir)
        paths = base.rglob("*.png") if recursive else base.glob("*.png")
        for source in paths:
            try:
                image = Image.open(source).convert("RGBA")
                alpha_mask = image.getchannel("A").point(lambda alpha: 255 if alpha > threshold else 0)
                bbox = alpha_mask.getbbox()
                if bbox is None:
                    result = image
                    unchanged += 1
                else:
                    left, top, right, bottom = bbox
                    box = (max(0, left - pad), max(0, top - pad),
                           min(image.width, right + pad), min(image.height, bottom + pad))
                    result = image.crop(box)
                    if result.size == image.size:
                        unchanged += 1
                    else:
                        changed += 1
                destination = source if in_place else Path(output_dir) / source.relative_to(base)
                destination.parent.mkdir(parents=True, exist_ok=True)
                result.save(destination)
            except Exception:
                failed += 1
        self.status.set(f"Transparent-edge strip: {changed} changed, {unchanged} unchanged, {failed} failed")
        messagebox.showinfo("Finished", self.status.get())

    def update_status(self):
        if self.image is None:
            return
        tw, th, cols, rows = self.dimensions()
        numbers = ", ".join(str(i) for i in sorted(self.selected)) or "none"
        self.status.set(f"{self.image.width}x{self.image.height} | {cols}x{rows} tiles of {tw}x{th} | Selected: {numbers}")


if __name__ == "__main__":
    SpriteSheetEditor().mainloop()
