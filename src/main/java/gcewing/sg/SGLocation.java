package gcewing.sg;

import gcewing.sg.block.entity.SGBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SGLocation {

    public final ResourceKey<Level> dimension;
    public final BlockPos pos;

    public SGLocation(BlockEntity te) {
        this(te.getLevel().dimension(), te.getBlockPos());
    }

    public SGLocation(ResourceKey<Level> dimension, BlockPos pos) {
        this.dimension = dimension;
        this.pos = pos;
    }

    public SGLocation(CompoundTag nbt) {
        dimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(nbt.getString("dimension")));
        pos = new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
    }

    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("dimension", dimension.location().toString());
        nbt.putInt("x", pos.getX());
        nbt.putInt("y", pos.getY());
        nbt.putInt("z", pos.getZ());
        return nbt;
    }

    /** The stargate at this location, or null. Never loads chunks. */
    public SGBaseBlockEntity getStargateTE() {
        ServerLevel world = SGAddressing.getWorld(dimension);
        if (world == null || !world.isLoaded(pos))
            return null;
        return world.getBlockEntity(pos) instanceof SGBaseBlockEntity te ? te : null;
    }
}
