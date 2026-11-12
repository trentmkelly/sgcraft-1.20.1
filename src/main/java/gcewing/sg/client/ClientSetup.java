package gcewing.sg.client;

import java.util.Map;

import gcewing.sg.ModRegistry;
import gcewing.sg.SGCraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.block.BlockModelShaper;

@Mod.EventBusSubscriber(modid = SGCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            MenuScreens.register(ModRegistry.SG_BASE_MENU.get(), SGBaseScreen::new);
            MenuScreens.register(ModRegistry.DHD_FUEL_MENU.get(), DHDFuelScreen::new);
            MenuScreens.register(ModRegistry.POWER_MENU.get(), PowerScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(ModRegistry.BASE_BE.get(), SGBaseRenderer::new);
        e.registerEntityRenderer(ModRegistry.IRIS.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void modifyBakingResult(ModelEvent.ModifyBakingResult e) {
        Map<ResourceLocation, BakedModel> models = e.getModels();
        for (Block block : new Block[] {ModRegistry.STARGATE_RING.get(), ModRegistry.STARGATE_CHEVRON.get(), ModRegistry.STARGATE_BASE.get()}) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                ModelResourceLocation loc = BlockModelShaper.stateToModelLocation(state);
                BakedModel model = models.get(loc);
                if (model != null && !(model instanceof CamouflageModel))
                    models.put(loc, new CamouflageModel(model));
            }
        }
    }
}
