-- Darkest Pixel Dungeon - Paper Doll Sprite Helper for Aseprite
-- Install: Copy this file to Aseprite's Scripts folder (File -> Scripts -> Open Scripts Folder),
-- then run via File -> Scripts -> aseprite_dpd_doll.

local function default_repo_root()
  -- Try to infer repo root if script is run from <repo>/dev/
  local info = debug.getinfo(1, "S")
  if info and info.source and info.source:sub(1, 1) == "@" then
    local path = info.source:sub(2)
    local dir = app.fs.filePath(path)
    if app.fs.fileName(dir) == "dev" then
      return app.fs.filePath(dir)
    end
  end
  return "/home/thecatist/darkest-pixel-dungeon"
end

local function quote(s)
  return '"' .. tostring(s):gsub('"', '\\"') .. '"'
end

local function run_doll_tool(repo, args_str)
  local tool = app.fs.joinPath(repo, "dev", "doll_tool.py")
  local cmd = "python3 " .. quote(tool) .. " " .. args_str
  local ok = os.execute(cmd)
  return ok, cmd
end

local function create_canvas_with_ref(repo, kind, hero_class, tier, fw, fh, num_frames)
  local tmp_guide = app.fs.joinPath(app.fs.tempPath, "dpd_guide_" .. kind .. ".png")
  local args = string.format(
    "guide --type %s --class %s --tier %d --frame-width %d --frame-height %d -o %s",
    kind, hero_class, tier, fw, fh, quote(tmp_guide)
  )
  local ok, cmd = run_doll_tool(repo, args)
  if not ok then
    app.alert("Failed to run doll_tool.py:\n" .. cmd)
    return
  end

  local spr = Sprite(fw, fh, ColorMode.RGB)
  spr.filename = (kind == "hat") and "dpd_hat.aseprite" or "dpd_weapon.aseprite"

  local ref_img = Image{ fromFile = tmp_guide }
  local ref_layer = spr.layers[1]
  ref_layer.name = "Ref_Hero"

  local draw_layer = spr:newLayer()
  draw_layer.name = (kind == "hat") and "Hat" or "Weapon"

  -- Populate frames
  local guide_cols = math.max(1, math.floor(ref_img.width / fw))
  for f = 1, num_frames do
    if f > 1 then
      spr:newEmptyFrame(f)
    end
    local g_idx = math.min(f - 1, guide_cols - 1)
    local cell = Image(fw, fh, ColorMode.RGB)
    cell:drawImage(ref_img, Point(-g_idx * fw, 0))
    spr:newCel(ref_layer, f, cell)
  end

  ref_layer.isEditable = false
  ref_layer.opacity = 160
  app.activeLayer = draw_layer
  app.activeFrame = spr.frames[1]
  app.refresh()
end

local function export_active_sprite(repo, kind, opts)
  local spr = app.activeSprite
  if not spr then
    app.alert("No active sprite!")
    return
  end

  -- Hide reference layer during export
  local ref_layer = nil
  local prev_vis = true
  for _, lyr in ipairs(spr.layers) do
    if lyr.name == "Ref_Hero" then
      ref_layer = lyr
      prev_vis = lyr.isVisible
      lyr.isVisible = false
    end
  end

  local tmp_strip = app.fs.joinPath(app.fs.tempPath, "dpd_export_strip.png")
  app.command.ExportSpriteSheet{
    ui = false,
    askOverwrite = false,
    type = SpriteSheetType.HORIZONTAL,
    columns = #spr.frames,
    rows = 1,
    textureFilename = tmp_strip,
    listLayers = false,
    listTags = false,
    listSlices = false,
  }

  if ref_layer then
    ref_layer.isVisible = prev_vis
  end

  local args
  if kind == "hat" then
    args = string.format("hat -i %s --row %d", quote(tmp_strip), opts.row)
  else
    args = string.format(
      "weapon -i %s --name %s --frame-width %d --frame-height %d --fps %s --offset-x %s --offset-y %s",
      quote(tmp_strip),
      quote(opts.name),
      spr.width,
      spr.height,
      tostring(opts.fps),
      tostring(opts.ox),
      tostring(opts.oy)
    )
    if opts.idle ~= "" then args = args .. " --idle " .. quote(opts.idle) end
    if opts.run ~= "" then args = args .. " --run " .. quote(opts.run) end
    if opts.attack ~= "" then args = args .. " --attack " .. quote(opts.attack) end
    if opts.extra ~= "" then
      for token in string.gmatch(opts.extra, "[^;]+") do
        local trimmed = token:match("^%s*(.-)%s*$")
        if trimmed ~= "" then
          args = args .. " --extra " .. quote(trimmed)
        end
      end
    end
  end

  local ok, cmd = run_doll_tool(repo, args)
  if not ok then
    app.alert("Export failed:\n" .. cmd)
    return
  end

  if opts.preview then
    local prev_path = app.fs.joinPath(app.fs.tempPath, "dpd_preview.gif")
    local target = (kind == "hat")
      and ("--helmet " .. tostring(opts.row))
      or ("--weapon " .. quote(opts.name))
    local p_args = string.format(
      "preview --class %s --tier %d %s --format gif -o %s",
      opts.hero_class, opts.tier, target, quote(prev_path)
    )
    if run_doll_tool(repo, p_args) then
      app.open(prev_path)
    end
  else
    app.alert("Exported successfully!")
  end
end

local dlg = Dialog("DPD Paper-Doll Helper")
local repo = default_repo_root()

dlg:entry{ id="repo", label="Repo Path:", text=repo }
dlg:combobox{ id="kind", label="Type:", option="weapon", options={"weapon", "hat"} }
dlg:combobox{ id="hero_class", label="Preview Class:", option="warrior",
              options={"warrior", "mage", "rogue", "huntress", "sorceress", "exile"} }
dlg:number{ id="tier", label="Armor Tier:", text="2", decimals=0 }

dlg:separator{ text="New Canvas" }
dlg:number{ id="fw", label="Frame W:", text="24", decimals=0 }
dlg:number{ id="fh", label="Frame H:", text="24", decimals=0 }
dlg:number{ id="frames", label="Frames:", text="5", decimals=0 }
dlg:button{ id="btn_new", text="Create Canvas + Ref Layer", onclick=function()
  local d = dlg.data
  local fw = (d.kind == "hat") and 12 or d.fw
  local fh = (d.kind == "hat") and 15 or d.fh
  create_canvas_with_ref(d.repo, d.kind, d.hero_class, d.tier, fw, fh, d.frames)
end }

dlg:separator{ text="Export Active Sprite" }
dlg:number{ id="row", label="Hat Row (helmets.png):", text="0", decimals=0 }
dlg:entry{ id="name", label="Weapon Name:", text="custom_weapon" }
dlg:number{ id="fps", label="Default FPS:", text="12", decimals=1 }
dlg:number{ id="ox", label="Offset X:", text="2", decimals=1 }
dlg:number{ id="oy", label="Offset Y:", text="0", decimals=1 }
dlg:entry{ id="idle", label="Idle (e.g. 0@1):", text="0@1" }
dlg:entry{ id="run", label="Run (e.g. 0,1@10):", text="" }
dlg:entry{ id="attack", label="Attack (e.g. 1,2,3,0@15):", text="" }
dlg:entry{ id="extra", label="Extra (skill=2,3@15;...):", text="" }
dlg:check{ id="preview", label="Open GIF Preview:", selected=true }
dlg:button{ id="btn_export", text="Export to DPD + Preview", onclick=function()
  local d = dlg.data
  export_active_sprite(d.repo, d.kind, d)
end }

dlg:show{ wait=false }
