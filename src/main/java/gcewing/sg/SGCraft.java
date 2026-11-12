//------------------------------------------------------------------------------------------------
//
//   SG Craft - Main Class
//
//   Port of Greg Ewing's SG Craft to Minecraft 1.20.1 / Forge.
//
//------------------------------------------------------------------------------------------------

package gcewing.sg;

import java.util.List;
import java.util.Set;

import gcewing.sg.network.SGNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SGCraft.MODID)
public class SGCraft {

    public static final String MODID = "sgcraft";

    public static boolean ccAvailable;

    public SGCraft() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SGConfig.SPEC);
        ModRegistry.register(modBus);
        ccAvailable = ModList.get().isLoaded("computercraft");
        if (ccAvailable)
            gcewing.sg.cc.CCIntegration.register(modBus);
        modBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.addListener(this::onVillagerTrades);
        MinecraftForge.EVENT_BUS.addListener(this::onLootTableLoad);
    }

    void commonSetup(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> {
            SGNetwork.init();
            SGChunkLoading.init();
        });
    }

    public static ResourceLocation loc(String path) {
        return new ResourceLocation(MODID, path);
    }

    // ------------------------------------------ Villagers ------------------------------------------

    void onVillagerTrades(VillagerTradesEvent e) {
        if (e.getType() == ModRegistry.TOKRA.get()) {
            List<VillagerTrades.ItemListing> level1 = e.getTrades().get(1);
            level1.add((trader, rand) -> new MerchantOffer(new ItemStack(Items.EMERALD, 8), new ItemStack(Items.DIAMOND),
                new ItemStack(ModRegistry.SG_CORE_CRYSTAL.get()), 7, 2, 0.05f));
            level1.add((trader, rand) -> new MerchantOffer(new ItemStack(Items.EMERALD, 16), new ItemStack(Items.DIAMOND),
                new ItemStack(ModRegistry.SG_CONTROLLER_CRYSTAL.get()), 7, 2, 0.05f));
        }
    }

    // ------------------------------------------ Loot -----------------------------------------------

    static final Set<String> lootTables = Set.of(
        "chests/simple_dungeon", "chests/spawn_bonus_chest", "chests/stronghold_library", "chests/village/village_weaponsmith");

    /** Adds the pools in data/sgcraft/loot_tables/inject/... to the matching vanilla chest tables. */
    void onLootTableLoad(LootTableLoadEvent e) {
        ResourceLocation name = e.getName();
        if (name.getNamespace().equals("minecraft") && lootTables.contains(name.getPath())) {
            e.getTable().addPool(LootPool.lootPool()
                .add(LootTableReference.lootTableReference(loc("inject/" + name.getPath())))
                .name(MODID + "_inject")
                .build());
        }
    }
}
