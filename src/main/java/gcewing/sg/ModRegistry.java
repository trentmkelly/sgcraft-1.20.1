package gcewing.sg;

import com.google.common.collect.ImmutableSet;
import gcewing.sg.block.DHDBlock;
import gcewing.sg.block.PowerUnitBlock;
import gcewing.sg.block.SGBaseBlock;
import gcewing.sg.block.SGRingBlock;
import gcewing.sg.block.entity.DHDBlockEntity;
import gcewing.sg.block.entity.PowerUnitBlockEntity;
import gcewing.sg.block.entity.SGBaseBlockEntity;
import gcewing.sg.block.entity.SGRingBlockEntity;
import gcewing.sg.entity.IrisEntity;
import gcewing.sg.item.SGUpgradeItem;
import gcewing.sg.menu.DHDFuelMenu;
import gcewing.sg.menu.PowerMenu;
import gcewing.sg.menu.SGBaseMenu;
import gcewing.sg.worldgen.NaquadahOreFeature;
import gcewing.sg.worldgen.PyramidStargateFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRegistry {

    public static final String MODID = SGCraft.MODID;

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, MODID);

    // ---------------------------------------- Blocks ----------------------------------------

    static BlockBehaviour.Properties gateProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
            .strength(1.5f, 6f).sound(SoundType.STONE).noOcclusion()
            .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false);
    }

    public static final RegistryObject<SGRingBlock> STARGATE_RING = BLOCKS.register("stargate_ring", () -> new SGRingBlock(gateProps(), false));
    public static final RegistryObject<SGRingBlock> STARGATE_CHEVRON = BLOCKS.register("stargate_chevron", () -> new SGRingBlock(gateProps(), true));
    public static final RegistryObject<SGBaseBlock> STARGATE_BASE = BLOCKS.register("stargate_base", () -> new SGBaseBlock(gateProps()));
    public static final RegistryObject<DHDBlock> STARGATE_CONTROLLER = BLOCKS.register("stargate_controller",
        () -> new DHDBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
            .strength(1.5f, 6f).sound(SoundType.STONE).noOcclusion()));
    public static final RegistryObject<Block> NAQUADAH_BLOCK = BLOCKS.register("naquadah_block",
        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).instrument(NoteBlockInstrument.BASEDRUM)
            .strength(5f, 10f).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> NAQUADAH_ORE = BLOCKS.register("naquadah_ore",
        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
            .strength(5f, 10f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistryObject<PowerUnitBlock> POWER_UNIT = BLOCKS.register("power_unit",
        () -> new PowerUnitBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
            .strength(1.5f, 10f).sound(SoundType.METAL)));

    // ---------------------------------------- Items ----------------------------------------

    public static final RegistryObject<Item> NAQUADAH = simpleItem("naquadah");
    public static final RegistryObject<Item> NAQUADAH_INGOT = simpleItem("naquadah_ingot");
    public static final RegistryObject<Item> SG_CORE_CRYSTAL = simpleItem("sg_core_crystal");
    public static final RegistryObject<Item> SG_CONTROLLER_CRYSTAL = simpleItem("sg_controller_crystal");
    public static final RegistryObject<Item> SG_CHEVRON_UPGRADE = ITEMS.register("sg_chevron_upgrade",
        () -> new SGUpgradeItem(new Item.Properties(), SGUpgradeItem.Kind.CHEVRON));
    public static final RegistryObject<Item> SG_IRIS_UPGRADE = ITEMS.register("sg_iris_upgrade",
        () -> new SGUpgradeItem(new Item.Properties(), SGUpgradeItem.Kind.IRIS));
    public static final RegistryObject<Item> SG_IRIS_BLADE = simpleItem("sg_iris_blade");
    public static final RegistryObject<Item> CAPACITOR = simpleItem("capacitor");

    static {
        blockItem(STARGATE_RING);
        blockItem(STARGATE_CHEVRON);
        blockItem(STARGATE_BASE);
        blockItem(STARGATE_CONTROLLER);
        blockItem(NAQUADAH_BLOCK);
        blockItem(NAQUADAH_ORE);
        blockItem(POWER_UNIT);
    }

    static RegistryObject<Item> simpleItem(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    public static RegistryObject<Item> blockItem(RegistryObject<? extends Block> block) {
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    // ------------------------------------ Block entities ------------------------------------

    public static final RegistryObject<BlockEntityType<SGRingBlockEntity>> RING_BE = BLOCK_ENTITIES.register("stargate_ring",
        () -> BlockEntityType.Builder.of(SGRingBlockEntity::new, STARGATE_RING.get(), STARGATE_CHEVRON.get()).build(null));
    public static final RegistryObject<BlockEntityType<SGBaseBlockEntity>> BASE_BE = BLOCK_ENTITIES.register("stargate_base",
        () -> BlockEntityType.Builder.of(SGBaseBlockEntity::new, STARGATE_BASE.get()).build(null));
    public static final RegistryObject<BlockEntityType<DHDBlockEntity>> DHD_BE = BLOCK_ENTITIES.register("stargate_controller",
        () -> BlockEntityType.Builder.of(DHDBlockEntity::new, STARGATE_CONTROLLER.get()).build(null));
    public static final RegistryObject<BlockEntityType<PowerUnitBlockEntity>> POWER_UNIT_BE = BLOCK_ENTITIES.register("power_unit",
        () -> BlockEntityType.Builder.of(PowerUnitBlockEntity::new, POWER_UNIT.get()).build(null));

    // ---------------------------------------- Menus ----------------------------------------

    public static final RegistryObject<MenuType<SGBaseMenu>> SG_BASE_MENU = MENUS.register("stargate_base",
        () -> IForgeMenuType.create(SGBaseMenu::new));
    public static final RegistryObject<MenuType<DHDFuelMenu>> DHD_FUEL_MENU = MENUS.register("dhd_fuel",
        () -> IForgeMenuType.create(DHDFuelMenu::new));
    public static final RegistryObject<MenuType<PowerMenu>> POWER_MENU = MENUS.register("power_unit",
        () -> IForgeMenuType.create(PowerMenu::new));

    // --------------------------------------- Entities ---------------------------------------

    public static final RegistryObject<EntityType<IrisEntity>> IRIS = ENTITIES.register("iris",
        () -> EntityType.Builder.<IrisEntity>of(IrisEntity::new, MobCategory.MISC)
            .sized(0.5f, 0.5f).noSummon().fireImmune().clientTrackingRange(10).updateInterval(100)
            .setShouldReceiveVelocityUpdates(false)
            .build(MODID + ":iris"));

    // ---------------------------------------- Sounds ----------------------------------------

    public static final RegistryObject<SoundEvent> SOUND_ABORT = sound("sg_abort");
    public static final RegistryObject<SoundEvent> SOUND_OPEN = sound("sg_open");
    public static final RegistryObject<SoundEvent> SOUND_CLOSE = sound("sg_close");
    public static final RegistryObject<SoundEvent> SOUND_DIAL = sound("sg_dial7");
    public static final RegistryObject<SoundEvent> SOUND_DIAL9 = sound("sg_dial9");
    public static final RegistryObject<SoundEvent> SOUND_IRIS_OPEN = sound("iris_open");
    public static final RegistryObject<SoundEvent> SOUND_IRIS_CLOSE = sound("iris_close");
    public static final RegistryObject<SoundEvent> SOUND_IRIS_HIT = sound("iris_hit");

    static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, name)));
    }

    // ---------------------------------------- Villager ---------------------------------------

    public static final RegistryObject<PoiType> TOKRA_POI = POI_TYPES.register("tokra",
        () -> new PoiType(ImmutableSet.copyOf(NAQUADAH_BLOCK.get().getStateDefinition().getPossibleStates()), 1, 1));
    public static final RegistryObject<VillagerProfession> TOKRA = PROFESSIONS.register("tokra",
        () -> new VillagerProfession("tokra",
            holder -> holder.is(TOKRA_POI.getKey()), holder -> holder.is(TOKRA_POI.getKey()),
            ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_CLERIC));

    // ---------------------------------------- Worldgen ---------------------------------------

    public static final RegistryObject<NaquadahOreFeature> NAQUADAH_ORE_FEATURE = FEATURES.register("naquadah_ore",
        () -> new NaquadahOreFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<PyramidStargateFeature> PYRAMID_STARGATE_FEATURE = FEATURES.register("pyramid_stargate",
        () -> new PyramidStargateFeature(NoneFeatureConfiguration.CODEC));

    // --------------------------------------- Creative tab --------------------------------------

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("sgcraft", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.sgcraft"))
        .icon(() -> new ItemStack(STARGATE_BASE.get()))
        .displayItems((params, out) -> {
            for (RegistryObject<Item> item : ITEMS.getEntries())
                out.accept(item.get());
        })
        .build());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        ENTITIES.register(bus);
        SOUNDS.register(bus);
        TABS.register(bus);
        POI_TYPES.register(bus);
        PROFESSIONS.register(bus);
        FEATURES.register(bus);
    }
}
