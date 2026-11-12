package gcewing.sg.cc;

import gcewing.sg.ModRegistry;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

/** ComputerCraft (CC: Tweaked) stargate interface. Only loaded when CC: Tweaked is present. */
public final class CCIntegration {

    public static RegistryObject<CCInterfaceBlock> CC_INTERFACE;
    public static RegistryObject<BlockEntityType<CCInterfaceBlockEntity>> CC_INTERFACE_BE;

    private CCIntegration() {
    }

    public static void register(IEventBus modBus) {
        CC_INTERFACE = ModRegistry.BLOCKS.register("cc_interface", () -> new CCInterfaceBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f, 10f).sound(SoundType.METAL)));
        ModRegistry.blockItem(CC_INTERFACE);
        CC_INTERFACE_BE = ModRegistry.BLOCK_ENTITIES.register("cc_interface",
            () -> BlockEntityType.Builder.of(CCInterfaceBlockEntity::new, CC_INTERFACE.get()).build(null));
    }
}
