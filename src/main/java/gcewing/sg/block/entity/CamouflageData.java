package gcewing.sg.block.entity;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

/** Model data used by the camouflage-aware stargate block model (safe to load on servers). */
public final class CamouflageData {

    public static final ModelProperty<Boolean> MERGED = new ModelProperty<>();
    public static final ModelProperty<BlockState> CAMOUFLAGE = new ModelProperty<>();

    private CamouflageData() {
    }

    public static ModelData of(boolean merged, BlockState camouflage) {
        ModelData.Builder b = ModelData.builder().with(MERGED, merged);
        if (camouflage != null)
            b.with(CAMOUFLAGE, camouflage);
        return b.build();
    }
}
