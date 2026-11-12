package gcewing.sg.entity;

import gcewing.sg.ModRegistry;
import gcewing.sg.block.entity.SGBaseBlockEntity;
import gcewing.sg.util.GateTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

/**
 * Invisible entity whose bounding box is solid while the iris of its stargate is closed.
 * (Ring interiors are air, so a block-based collision shape isn't possible.)
 */
public class IrisEntity extends Entity implements IEntityAdditionalSpawnData {

    public BlockPos basePos = BlockPos.ZERO;
    AABB box;

    public IrisEntity(EntityType<? extends IrisEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public IrisEntity(SGBaseBlockEntity te) {
        this(ModRegistry.IRIS.get(), te.getLevel());
        double radius = 2;
        double thickness = SGBaseBlockEntity.irisThickness;
        double cz = SGBaseBlockEntity.irisZPosition;
        GateTransform t = te.localToGlobalTransformation();
        AABB globalBox = t.box(new Vec3(-radius, 2 - radius, cz - thickness), new Vec3(radius, 2 + radius, cz + thickness));
        init(te.getBlockPos(), globalBox);
    }

    void init(BlockPos pos, AABB box) {
        this.basePos = pos;
        this.box = box;
        Vec3 c = box.getCenter();
        setPos(c.x, c.y, c.z);
        setBoundingBox(box);
    }

    @Override
    protected AABB makeBoundingBox() {
        return box != null ? box : super.makeBoundingBox();
    }

    @Override
    protected void defineSynchedData() {
    }

    SGBaseBlockEntity getBaseTE() {
        if (level().isLoaded(basePos) && level().getBlockEntity(basePos) instanceof SGBaseBlockEntity te)
            return te;
        return null;
    }

    @Override
    public void tick() {
        // Stationary; only check that our stargate still wants us.
        if (!level().isClientSide && tickCount % 100 == 0) {
            SGBaseBlockEntity te = getBaseTE();
            if (level().isLoaded(basePos) && (te == null || !te.isMerged || !te.hasIrisUpgrade))
                discard();
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        SGBaseBlockEntity te = getBaseTE();
        return te != null && te.irisIsClosed();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        BlockPos pos = new BlockPos(nbt.getInt("blockX"), nbt.getInt("blockY"), nbt.getInt("blockZ"));
        AABB b = new AABB(nbt.getDouble("minX"), nbt.getDouble("minY"), nbt.getDouble("minZ"),
            nbt.getDouble("maxX"), nbt.getDouble("maxY"), nbt.getDouble("maxZ"));
        init(pos, b);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("blockX", basePos.getX());
        nbt.putInt("blockY", basePos.getY());
        nbt.putInt("blockZ", basePos.getZ());
        AABB b = getBoundingBox();
        nbt.putDouble("minX", b.minX);
        nbt.putDouble("minY", b.minY);
        nbt.putDouble("minZ", b.minZ);
        nbt.putDouble("maxX", b.maxX);
        nbt.putDouble("maxY", b.maxY);
        nbt.putDouble("maxZ", b.maxZ);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buf) {
        buf.writeBlockPos(basePos);
        AABB b = getBoundingBox();
        buf.writeDouble(b.minX);
        buf.writeDouble(b.minY);
        buf.writeDouble(b.minZ);
        buf.writeDouble(b.maxX);
        buf.writeDouble(b.maxY);
        buf.writeDouble(b.maxZ);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        AABB b = new AABB(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
        init(pos, b);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
