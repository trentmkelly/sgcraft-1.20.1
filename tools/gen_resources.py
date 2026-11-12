#!/usr/bin/env python3
"""Generate the JSON assets and data files for the port.

Usage: python3 -I tools/gen_resources.py <path/to/repo>
"""
import json
import os
import sys

repo = sys.argv[1]
res = os.path.join(repo, "src/main/resources")
A = os.path.join(res, "assets/sgcraft")
D = os.path.join(res, "data")
NS = "sgcraft"


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


# Rotations for blocks whose model faces south when FACING=north (see GateTransform).
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}

# ------------------------------------------------------------------ block models

def cube(name, down, up, north, south, west, east, particle=None):
    write(f"{A}/models/block/{name}.json", {
        "parent": "minecraft:block/cube",
        "textures": {
            "particle": particle or north, "down": down, "up": up,
            "north": north, "south": south, "west": west, "east": east,
        },
    })


t = lambda n: f"{NS}:block/{n}"
cube("stargate_ring", t("stargate_block"), t("stargate_block"), t("stargate_ring"), t("stargate_ring"),
     t("stargate_ring"), t("stargate_ring"))
cube("stargate_chevron", t("stargate_block"), t("stargate_block"), t("stargate_chevron"), t("stargate_chevron"),
     t("stargate_chevron"), t("stargate_chevron"))
cube("stargate_base", t("stargate_block"), t("stargate_block"), t("stargate_ring"), t("stargate_base_front"),
     t("stargate_ring"), t("stargate_ring"))
cube("power_unit", t("power_unit_bottom"), t("power_unit_top"), t("power_unit_side"), t("power_unit_side"),
     t("power_unit_side"), t("power_unit_side"))
cube("cc_interface", t("cc_interface_bottom"), t("cc_interface_top"), t("cc_interface_side"), t("cc_interface_front"),
     t("cc_interface_side"), t("cc_interface_side"))
for n in ("naquadah_block", "naquadah_ore"):
    write(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": t(n)}})

for button in ("off", "idle", "lit"):
    write(f"{A}/models/block/stargate_controller_{button}.json", {
        "loader": "forge:obj",
        "model": f"{NS}:models/block/dhd.obj",
        "mtl_override": f"{NS}:models/block/{'dhd_lit' if button == 'lit' else 'dhd'}.mtl",
        "flip_v": False,
        "emissive_ambient": True,
        "automatic_culling": False,
        "shade_quads": True,
        "textures": {
            "particle": t("dhd_side"),
            "top": t("dhd_top"),
            "side": t("dhd_side"),
            "detail": t("dhd_detail"),
            "button": t(f"dhd_button_{button}"),
        },
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, -1.5, 0], "scale": [0.5, 0.5, 0.5]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, -2, 0], "scale": [0.5, 0.5, 0.5]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
        },
    })

# ------------------------------------------------------------------ blockstates

for n in ("stargate_ring", "stargate_chevron", "naquadah_block", "naquadah_ore", "power_unit"):
    write(f"{A}/blockstates/{n}.json", {"variants": {"": {"model": f"{NS}:block/{n}"}}})

for n in ("stargate_base", "cc_interface"):
    write(f"{A}/blockstates/{n}.json", {"variants": {
        f"facing={f}": ({"model": f"{NS}:block/{n}", "y": y} if y else {"model": f"{NS}:block/{n}"})
        for f, y in FACING_Y.items()}})

variants = {}
for f, y in FACING_Y.items():
    for button in ("off", "idle", "lit"):
        v = {"model": f"{NS}:block/stargate_controller_{button}"}
        if y:
            v["y"] = y
        variants[f"button={button},facing={f}"] = v
write(f"{A}/blockstates/stargate_controller.json", {"variants": variants})

# ------------------------------------------------------------------ item models

for n in ("stargate_ring", "stargate_chevron", "stargate_base", "naquadah_block", "naquadah_ore", "power_unit",
          "cc_interface"):
    write(f"{A}/models/item/{n}.json", {"parent": f"{NS}:block/{n}"})
write(f"{A}/models/item/stargate_controller.json", {"parent": f"{NS}:block/stargate_controller_idle"})
for n in ("naquadah", "naquadah_ingot", "sg_core_crystal", "sg_controller_crystal", "sg_chevron_upgrade",
          "sg_iris_upgrade", "sg_iris_blade", "capacitor"):
    write(f"{A}/models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{n}"}})

# ------------------------------------------------------------------ sounds

write(f"{A}/sounds.json", {n: {"sounds": [f"{NS}:{n}"], "subtitle": f"subtitles.{NS}.{n}"} for n in (
    "sg_abort", "sg_close", "sg_dial7", "sg_dial9", "sg_open", "iris_open", "iris_close", "iris_hit")})

# ------------------------------------------------------------------ language

lang = {
    "block.sgcraft.stargate_ring": "Stargate Ring Block",
    "block.sgcraft.stargate_chevron": "Stargate Chevron Block",
    "block.sgcraft.stargate_base": "Stargate Base",
    "block.sgcraft.stargate_controller": "Stargate Controller",
    "block.sgcraft.naquadah_block": "Naquadah Alloy Block",
    "block.sgcraft.naquadah_ore": "Naquadah Ore",
    "block.sgcraft.power_unit": "Stargate Power Unit",
    "block.sgcraft.cc_interface": "ComputerCraft Stargate Interface",
    "item.sgcraft.naquadah": "Naquadah",
    "item.sgcraft.naquadah_ingot": "Naquadah Alloy Ingot",
    "item.sgcraft.sg_core_crystal": "Stargate Core Crystal",
    "item.sgcraft.sg_controller_crystal": "Stargate Controller Crystal",
    "item.sgcraft.sg_chevron_upgrade": "Stargate Chevron Upgrade",
    "item.sgcraft.sg_iris_upgrade": "Stargate Iris Upgrade",
    "item.sgcraft.sg_iris_blade": "Stargate Iris Blade",
    "item.sgcraft.capacitor": "Ridiculously Large Capacitor",
    "entity.sgcraft.iris": "Stargate Iris",
    "entity.minecraft.villager.sgcraft.tokra": "Tok'ra Merchant",
    "entity.minecraft.villager.tokra": "Tok'ra Merchant",
    "death.attack.sgcraft.transient": "%1$s was torn apart by an event horizon",
    "death.attack.sgcraft.iris": "%1$s got splattered against a stargate iris",
    "itemGroup.sgcraft": "SG Craft",
    "gui.sgcraft.stargate_address": "Stargate Address",
    "gui.sgcraft.base_camouflage": "Base Camouflage",
    "gui.sgcraft.stargate_controller": "Stargate Controller",
    "gui.sgcraft.fuel": "Fuel",
    "gui.sgcraft.power_unit": "Stargate Power Unit",
    "gui.sgcraft.max": "Max",
    "message.sgcraft.disconnect_before_breaking": "Disconnect stargate before breaking",
    "subtitles.sgcraft.sg_abort": "Stargate dialling aborted",
    "subtitles.sgcraft.sg_close": "Stargate closes",
    "subtitles.sgcraft.sg_dial7": "Stargate ring turns",
    "subtitles.sgcraft.sg_dial9": "Stargate ring turns",
    "subtitles.sgcraft.sg_open": "Stargate opens",
    "subtitles.sgcraft.iris_open": "Iris opens",
    "subtitles.sgcraft.iris_close": "Iris closes",
    "subtitles.sgcraft.iris_hit": "Something hits an iris",
}
write(f"{A}/lang/en_us.json", lang)

# ------------------------------------------------------------------ data: loot tables

def self_drop(name):
    return {"type": "minecraft:block", "pools": [{
        "rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]}


for n in ("stargate_ring", "stargate_chevron", "stargate_base", "stargate_controller", "naquadah_block", "cc_interface"):
    write(f"{D}/{NS}/loot_tables/blocks/{n}.json", self_drop(n))

write(f"{D}/{NS}/loot_tables/blocks/power_unit.json", {"type": "minecraft:block", "pools": [{
    "rolls": 1,
    "entries": [{
        "type": "minecraft:item", "name": f"{NS}:power_unit",
        "functions": [{
            "function": "minecraft:copy_nbt", "source": "block_entity",
            "ops": [{"source": "energyBuffer", "target": "BlockEntityTag.energyBuffer", "op": "replace"}]}]}],
    "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

silk = {"condition": "minecraft:match_tool", "predicate": {"enchantments": [
    {"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}
write(f"{D}/{NS}/loot_tables/blocks/naquadah_ore.json", {"type": "minecraft:block", "pools": [{
    "rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": f"{NS}:naquadah_ore", "conditions": [silk]},
        {"type": "minecraft:item", "name": f"{NS}:naquadah", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 6}},
            {"function": "minecraft:explosion_decay"}]}]}]}]})


def item(name, weight=1, count=None):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    if count:
        e["functions"] = [{"function": "minecraft:set_count",
                           "count": {"type": "minecraft:uniform", "min": count[0], "max": count[1]}}]
    return e


def pool(chance, rolls, entries):
    return {"rolls": rolls if isinstance(rolls, int) else {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]},
            "conditions": [{"condition": "minecraft:random_chance", "chance": chance}],
            "entries": entries}


inject = {
    "chests/simple_dungeon": [pool(0.2, (1, 2), [
        item(f"{NS}:naquadah", 2, (1, 8)), item("minecraft:ender_pearl", 2),
        item(f"{NS}:sg_core_crystal", 2), item(f"{NS}:sg_controller_crystal", 1)])],
    "chests/spawn_bonus_chest": [
        pool(1.0, (1, 2), [item(f"{NS}:sg_core_crystal", 1, (1, 2)), item(f"{NS}:sg_controller_crystal", 1, (1, 2))]),
        pool(0.5, 1, [item(f"{NS}:naquadah_ingot", 1, (32, 64))]),
        pool(0.75, 1, [item(f"{NS}:naquadah", 1, (8, 16))]),
        pool(0.5, 1, [item("minecraft:ender_pearl", 1, (6, 24))])],
    "chests/stronghold_library": [pool(0.75, (1, 2), [
        item(f"{NS}:sg_core_crystal", 1), item(f"{NS}:sg_controller_crystal", 2)])],
    # The village blacksmith chest was split up in 1.14; the weaponsmith is its successor.
    "chests/village/village_weaponsmith": [pool(0.5, (1, 2), [
        item(f"{NS}:naquadah_ingot", 2, (1, 8)), item(f"{NS}:sg_core_crystal", 1),
        item(f"{NS}:sg_controller_crystal", 1)])],
}
for name, pools in inject.items():
    write(f"{D}/{NS}/loot_tables/inject/{name}.json", {"type": "minecraft:chest", "pools": pools})

# ------------------------------------------------------------------ data: recipes

def shaped(name, result, pattern, key, count=1, conditions=None):
    r = {"type": "minecraft:crafting_shaped", "pattern": pattern,
         "key": {k: v for k, v in key.items()}, "result": {"item": result, "count": count}}
    if conditions:
        r = {"type": "forge:conditional", "recipes": [{"conditions": conditions, "recipe": r}]}
    write(f"{D}/{NS}/recipes/{name}.json", r)


def shapeless(name, result, ingredients, count=1, conditions=None):
    r = {"type": "minecraft:crafting_shapeless", "ingredients": ingredients, "result": {"item": result, "count": count}}
    if conditions:
        r = {"type": "forge:conditional", "recipes": [{"conditions": conditions, "recipe": r}]}
    write(f"{D}/{NS}/recipes/{name}.json", r)


I = lambda n: {"item": n}
TAG = lambda n: {"tag": n}
alloy = TAG("forge:ingots/naquadah_alloy")
smooth = I("minecraft:cut_sandstone")       # 1.10 "smooth sandstone" (meta 2) is cut sandstone now
chiseled = I("minecraft:chiseled_sandstone")

shaped("stargate_ring", f"{NS}:stargate_ring", ["CCC", "NNN", "SSS"], {"S": smooth, "N": alloy, "C": chiseled})
shaped("stargate_chevron", f"{NS}:stargate_chevron", ["CgC", "NpN", "SrS"], {
    "S": smooth, "N": alloy, "C": chiseled, "g": I("minecraft:glowstone_dust"), "r": I("minecraft:redstone"),
    "p": I("minecraft:ender_pearl")})
shaped("stargate_base", f"{NS}:stargate_base", ["CrC", "NeN", "ScS"], {
    "S": smooth, "N": alloy, "C": chiseled, "r": I("minecraft:redstone"), "e": I("minecraft:ender_eye"),
    "c": I(f"{NS}:sg_core_crystal")})
shaped("stargate_controller", f"{NS}:stargate_controller", ["bbb", "OpO", "OcO"], {
    "b": I("minecraft:stone_button"), "O": I("minecraft:obsidian"), "p": I("minecraft:ender_pearl"),
    "c": I(f"{NS}:sg_controller_crystal")})
shapeless("naquadah_ingot", f"{NS}:naquadah_ingot", [TAG("forge:gems/naquadah"), I("minecraft:iron_ingot")])
shaped("naquadah_block", f"{NS}:naquadah_block", ["NNN", "NNN", "NNN"], {"N": alloy})
shapeless("naquadah_ingot_from_block", f"{NS}:naquadah_ingot", [I(f"{NS}:naquadah_block")], count=9)
shaped("sg_chevron_upgrade", f"{NS}:sg_chevron_upgrade", ["g g", "pNp", "r r"], {
    "N": alloy, "g": I("minecraft:glowstone_dust"), "r": I("minecraft:redstone"), "p": I("minecraft:ender_pearl")})
shaped("sg_iris_blade", f"{NS}:sg_iris_blade", [" ii", "ic ", "i  "], {
    "i": I("minecraft:iron_ingot"), "c": I("minecraft:charcoal")})
shaped("sg_iris_upgrade", f"{NS}:sg_iris_upgrade", ["bbb", "brb", "bbb"], {
    "b": I(f"{NS}:sg_iris_blade"), "r": I("minecraft:redstone")})
shaped("capacitor", f"{NS}:capacitor", ["iii", "ppp", "iii"], {"i": TAG("forge:ingots/iron"), "p": I("minecraft:paper")})
shaped("power_unit", f"{NS}:power_unit", ["cgc", "gIg", "crc"], {
    "c": I(f"{NS}:capacitor"), "g": TAG("forge:ingots/gold"), "I": TAG("forge:storage_blocks/iron"),
    "r": I("minecraft:redstone")})
shaped("cc_interface", f"{NS}:cc_interface", ["SnS", "SrS", "SSS"], {
    "S": I("minecraft:stone"), "n": alloy, "r": I("minecraft:redstone")},
    conditions=[{"type": "forge:mod_loaded", "modid": "computercraft"}])
# Optional recipes the original enabled with config options; here they are data, so a datapack can
# remove them. They stay off by default as in the original (allowCraftingNaquadah/Crystals = false).

# ------------------------------------------------------------------ data: tags

def tag(path, values):
    write(f"{D}/{path}.json", {"replace": False, "values": values})


tag("minecraft/tags/blocks/mineable/pickaxe", [f"{NS}:{n}" for n in (
    "stargate_ring", "stargate_chevron", "stargate_base", "stargate_controller", "naquadah_block", "naquadah_ore",
    "power_unit")] + [{"id": f"{NS}:cc_interface", "required": False}])
tag("minecraft/tags/blocks/needs_diamond_tool", [f"{NS}:naquadah_ore"])
tag("minecraft/tags/blocks/needs_iron_tool", [f"{NS}:naquadah_block"])
tag("forge/tags/blocks/ores", [f"{NS}:naquadah_ore"])
tag("forge/tags/blocks/ores/naquadah", [f"{NS}:naquadah_ore"])
tag("forge/tags/items/ores", [f"{NS}:naquadah_ore"])
tag("forge/tags/items/ores/naquadah", [f"{NS}:naquadah_ore"])
tag("forge/tags/items/gems/naquadah", [f"{NS}:naquadah"])
tag("forge/tags/items/ingots/naquadah_alloy", [f"{NS}:naquadah_ingot"])
tag("forge/tags/items/storage_blocks/naquadah_alloy", [f"{NS}:naquadah_block"])
tag("forge/tags/blocks/storage_blocks/naquadah_alloy", [f"{NS}:naquadah_block"])
tag("minecraft/tags/point_of_interest_type/acquirable_job_site", [f"{NS}:tokra"])

# ------------------------------------------------------------------ data: damage types

write(f"{D}/{NS}/damage_type/transient.json", {"message_id": f"{NS}.transient", "exhaustion": 0.0, "scaling": "never"})
write(f"{D}/{NS}/damage_type/iris.json", {"message_id": f"{NS}.iris", "exhaustion": 0.0, "scaling": "never"})
tag("minecraft/tags/damage_type/bypasses_armor", [f"{NS}:iris"])

# ------------------------------------------------------------------ data: worldgen

write(f"{D}/{NS}/worldgen/configured_feature/naquadah_ore.json", {"type": f"{NS}:naquadah_ore", "config": {}})
write(f"{D}/{NS}/worldgen/placed_feature/naquadah_ore.json", {"feature": f"{NS}:naquadah_ore", "placement": []})
write(f"{D}/{NS}/worldgen/configured_feature/pyramid_stargate.json", {"type": f"{NS}:pyramid_stargate", "config": {}})
write(f"{D}/{NS}/worldgen/placed_feature/pyramid_stargate.json", {"feature": f"{NS}:pyramid_stargate", "placement": []})
for dim in ("overworld", "nether", "end"):
    write(f"{D}/{NS}/forge/biome_modifier/naquadah_ore_{dim}.json", {
        "type": "forge:add_features", "biomes": f"#minecraft:is_{dim}",
        "features": f"{NS}:naquadah_ore", "step": "underground_ores"})
write(f"{D}/{NS}/forge/biome_modifier/pyramid_stargate.json", {
    "type": "forge:add_features", "biomes": "#minecraft:has_structure/desert_pyramid",
    "features": f"{NS}:pyramid_stargate", "step": "top_layer_modification"})

print("ok")
