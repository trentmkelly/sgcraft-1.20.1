#!/usr/bin/env python3
"""Import assets from the original SG Craft 1.10.2 source release into this port.

Usage: python3 -I tools/import_assets.py <path/to/src-1.10.2/src/resources/assets/sgcraft> <path/to/repo>

Renames textures to 1.13+ snake_case paths, splits/tints the DHD detail texture and
converts Greg's .smeg DHD mesh to Wavefront OBJ for Forge's OBJ model loader.
"""
import json
import os
import shutil
import sys

from PIL import Image

src, repo = sys.argv[1], sys.argv[2]
dst = os.path.join(repo, "src/main/resources/assets/sgcraft")


def copy(rel_src, rel_dst):
    out = os.path.join(dst, rel_dst)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    shutil.copyfile(os.path.join(src, rel_src), out)


blocks = {
    "stargateBlock": "stargate_block",
    "stargateRing": "stargate_ring",
    "stargateChevron": "stargate_chevron",
    "stargateBase_front": "stargate_base_front",
    "naquadahBlock": "naquadah_block",
    "naquadahOre": "naquadah_ore",
    "rfPowerUnit-top": "power_unit_top",
    "rfPowerUnit-bottom": "power_unit_bottom",
    "rfPowerUnit-side": "power_unit_side",
}
for k, v in blocks.items():
    copy(f"textures/blocks/{k}.png", f"textures/block/{v}.png")

items = {
    "naquadah": "naquadah",
    "naquadahIngot": "naquadah_ingot",
    "sgCoreCrystal": "sg_core_crystal",
    "sgControllerCrystal": "sg_controller_crystal",
    "sgChevronUpgrade": "sg_chevron_upgrade",
    "sgIrisUpgrade": "sg_iris_upgrade",
    "sgIrisBlade": "sg_iris_blade",
    "ic2Capacitor": "capacitor",
}
for k, v in items.items():
    copy(f"textures/items/{k}.png", f"textures/item/{v}.png")

for name in os.listdir(os.path.join(src, "textures/gui")):
    if name.endswith(".png"):
        copy(f"textures/gui/{name}", f"textures/gui/{name}")

for name in ("stargate", "eventhorizon", "iris"):
    copy(f"textures/tileentity/{name}.png", f"textures/entity/{name}.png")

copy("textures/skins/tokra.png", "textures/entity/villager/profession/tokra.png")

for name in os.listdir(os.path.join(src, "sounds")):
    if name.endswith(".ogg"):
        copy(f"sounds/{name}", f"sounds/{name}")

# --- DHD textures -------------------------------------------------------------------------
copy("textures/tileentity/dhd_top.png", "textures/block/dhd_top.png")
copy("textures/tileentity/dhd_side.png", "textures/block/dhd_side.png")
detail = Image.open(os.path.join(src, "textures/tileentity/dhd_detail.png")).convert("RGBA")
w, h = detail.size
tw, th = w // 2, h // 2
detail.crop((tw, th, w, h)).save(os.path.join(dst, "textures/block/dhd_detail.png"))
button = detail.crop((0, 0, tw, th))
# Same tints the original DHD renderer applied to the button tile.
for suffix, (r, g, b) in {"off": (0.5, 0.5, 0.5), "idle": (0.5, 0.25, 0.0), "lit": (1.0, 0.5, 0.0)}.items():
    px = button.copy()
    data = [(int(pr * r), int(pg * g), int(pb * b), pa) for (pr, pg, pb, pa) in px.getdata()]
    px.putdata(data)
    px.save(os.path.join(dst, f"textures/block/dhd_button_{suffix}.png"))

# --- DHD mesh -----------------------------------------------------------------------------
smeg = json.load(open(os.path.join(src, "models/dhd.smeg")))
materials = ["top", "side", "detail", "button"]
models_dir = os.path.join(dst, "models/block")
os.makedirs(models_dir, exist_ok=True)
lines = ["# Converted from SG Craft dhd.smeg by tools/import_assets.py", "mtllib dhd.mtl"]
vi = 1
by_mat = {i: [] for i in range(4)}
for face in smeg["faces"]:
    by_mat[face["texture"]].append(face)
for t, faces in by_mat.items():
    lines.append(f"usemtl {materials[t]}")
    for face in faces:
        base = vi
        for (x, y, z, nx, ny, nz, u, v) in face["vertices"]:
            # smeg origin is the block's bottom centre; OBJ block space has its origin at the corner.
            lines.append(f"v {x + 0.5:.6f} {y:.6f} {z + 0.5:.6f}")
            lines.append(f"vt {u:.6f} {v:.6f}")
            lines.append(f"vn {nx:.6f} {ny:.6f} {nz:.6f}")
            vi += 1
        for tri in face["triangles"]:
            a, b, c = (base + i for i in tri)
            lines.append(f"f {a}/{a}/{a} {b}/{b}/{b} {c}/{c}/{c}")
open(os.path.join(models_dir, "dhd.obj"), "w").write("\n".join(lines) + "\n")

mtl = []
for m in materials:
    mtl += [f"newmtl {m}", "Ka 0 0 0", "Kd 1 1 1", f"map_Kd #{m}", ""]
open(os.path.join(models_dir, "dhd.mtl"), "w").write("\n".join(mtl))
# Lit variant: ambient colour on the button makes Forge's OBJ loader render it full-bright.
mtl_lit = []
for m in materials:
    ka = "1 1 1" if m == "button" else "0 0 0"
    mtl_lit += [f"newmtl {m}", f"Ka {ka}", "Kd 1 1 1", f"map_Kd #{m}", ""]
open(os.path.join(models_dir, "dhd_lit.mtl"), "w").write("\n".join(mtl_lit))
print("ok")
