package gcewing.sg.block.entity;

import gcewing.sg.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

public class SGRingBlockEntity extends SyncedBlockEntity {

    public boolean isMerged;
    public BlockPos basePos = BlockPos.ZERO;

    public SGRingBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.RING_BE.get(), pos, state);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        isMerged = nbt.getBoolean("isMerged");
        basePos = new BlockPos(nbt.getInt("baseX"), nbt.getInt("baseY"), nbt.getInt("baseZ"));
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putBoolean("isMerged", isMerged);
        nbt.putInt("baseX", basePos.getX());
        nbt.putInt("baseY", basePos.getY());
        nbt.putInt("baseZ", basePos.getZ());
    }

    public SGBaseBlockEntity getBaseTE() {
        if (isMerged && level != null) {
            BlockEntity bte = level.getBlockEntity(basePos);
            if (bte instanceof SGBaseBlockEntity base)
                return base;
        }
        return null;
    }

    @Override
    protected void onClientUpdate() {
        requestModelDataUpdate();
        if (level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
    }

    @Override
    public ModelData getModelData() {
        SGBaseBlockEntity base = getBaseTE();
        return CamouflageData.of(isMerged, base != null ? base.getCamouflageState(worldPosition) : null);
    }
}
