# SG Craft for Minecraft 1.20.1

A port of [Greg's SG Craft](http://www.cosc.canterbury.ac.nz/greg.ewing/minecraft/mods/SGCraft/)
by Greg Ewing, which adds SG-1 style stargates to Minecraft, to Minecraft 1.20.1 (Forge 47).

The port is based on Greg's official SG Craft 1.13.3 source release (1.7.10) and 1.13.2
(1.10.2). It plays the same as the original. The changes listed below were forced by
Minecraft or mod-ecosystem changes since then.

## Differences from the original

- **Block and item IDs.** These follow the 1.13+ flattening: the old `stargateRing` block with a
  ring/chevron variant is now two blocks (`stargate_ring` and `stargate_chevron`), and every ID is
  snake_case. Worlds from older versions can't be converted.
- **Power.** The RF and IC2 power units are replaced by a single **Stargate Power Unit** that
  accepts Forge Energy (FE), the successor to RF. It uses the same numbers as the RF unit:
  80 FE per stargate energy unit and 4,000,000 FE capacity. It's crafted with the generic
  "Ridiculously Large Capacitor" recipe.
- **Mod integrations.** ComputerCraft support uses **CC: Tweaked** (optional), with the same
  `stargate` peripheral methods and events. OpenComputers, IndustrialCraft 2, Mystcraft and Thermal
  Expansion support is dropped, because none of them exist for 1.20.1.
- **Configuration.** Settings are in `config/sgcraft-common.toml`, with the original option names
  and defaults. `addOresToExistingWorlds` is gone. `allowCraftingNaquadah` and
  `allowCraftingCrystals` are now off-by-default recipes, so a datapack has to add them.
- **Naquadah ore.** Ore spawns in the same pattern as before (under lava and in isolated nodes)
  in all three dimensions. Heights are measured from the bottom of the world, since worlds now
  go below y=0.
- **Buried stargates under desert pyramids** (`augmentStructures`, still off by default). Since
  1.20, desert pyramids have a cellar and a deeper trap room where the original chamber was.
  The chamber is now built further down, under the pyramid, and reached by a ladder shaft from
  the main hall floor.
- **Tok'ra villagers.** Villager professions now need a workstation, so a villager becomes a
  Tok'ra by claiming a **Naquadah Alloy Block**. The trades are the original ones.
- **Chest loot.** The village blacksmith chest was split up in 1.14. The SG Craft items that used
  to appear there now appear in the weaponsmith's chest.
- **Camouflage.** Blocks in the stargate's camouflage slots always use their default state,
  since items no longer carry block metadata.
- **Addresses.** Dimensions are identified by name rather than number. Each world numbers its
  dimensions in the order they're first used, except that the Overworld, Nether and End always
  come first, so a given location in those three has the same address in every world.

## Building

`./gradlew build` produces `build/libs/SGCraft-<version>.jar`. Development runs
(`./gradlew runClient` / `runServer`) use `../dev-run`, outside the repository.

Port done entirely by Claude Opus 5.5.

## Licence

The original SG Craft is free software: "You are free to do whatever you want with it."
See [LICENSE](LICENSE) for the quoted terms. This port uses the same terms.
