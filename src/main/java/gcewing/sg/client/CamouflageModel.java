package gcewing.sg.client;

import java.util.List;

import gcewing.sg.block.entity.CamouflageData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Model for stargate ring/base blocks. Once merged into a gate the blocks are drawn by the gate
 * renderer instead, so they render nothing - or, in the bottom row, the camouflage block chosen
 * in the gate's GUI. (Replaces Greg's SGRingBlockRenderer custom block renderer.)
 */
public class CamouflageModel extends BakedModelWrapper<BakedModel> {

    public CamouflageModel(BakedModel original) {
        super(original);
    }

    static boolean merged(ModelData data) {
        Boolean m = data.get(CamouflageData.MERGED);
        return m != null && m;
    }

    static BakedModel camoModel(BlockState camo) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(camo);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                             @NotNull ModelData data, @Nullable RenderType renderType) {
        if (!merged(data))
            return super.getQuads(state, side, rand, data, renderType);
        BlockState camo = data.get(CamouflageData.CAMOUFLAGE);
        if (camo == null)
            return List.of();
        BakedModel model = camoModel(camo);
        if (renderType != null && !model.getRenderTypes(camo, rand, ModelData.EMPTY).contains(renderType))
            return List.of();
        return model.getQuads(camo, side, rand, ModelData.EMPTY, renderType);
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        if (!merged(data))
            return super.getRenderTypes(state, rand, data);
        BlockState camo = data.get(CamouflageData.CAMOUFLAGE);
        if (camo == null)
            return ChunkRenderTypeSet.none();
        return camoModel(camo).getRenderTypes(camo, rand, ModelData.EMPTY);
    }
}
