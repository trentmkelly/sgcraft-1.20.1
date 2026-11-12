package gcewing.sg.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Local-to-global transformation for an oriented block, replacing Greg's Trans3 for the
 * 4-way orientations used by SG Craft.
 *
 * The block's FACING is the horizontal direction the placing player was looking. In local
 * coordinates +z points back towards the player (out of the gate's front), +y is up and +x
 * is FACING rotated clockwise. The origin is the block centre unless specified otherwise.
 */
public final class GateTransform {

    public final Vec3 origin;
    public final Direction facing;
    private final int xx, xz, zx, zz; // global = origin + (lx * X + ly * Y + lz * Z)

    public GateTransform(Vec3 origin, Direction facing) {
        this.origin = origin;
        this.facing = facing;
        Direction xAxis = facing.getClockWise();
        Direction zAxis = facing.getOpposite();
        xx = xAxis.getStepX();
        xz = xAxis.getStepZ();
        zx = zAxis.getStepX();
        zz = zAxis.getStepZ();
    }

    public static GateTransform blockCenter(BlockPos pos, Direction facing) {
        return new GateTransform(Vec3.atCenterOf(pos), facing);
    }

    /** Local point to global point. */
    public Vec3 p(double x, double y, double z) {
        return new Vec3(origin.x + x * xx + z * zx, origin.y + y, origin.z + x * xz + z * zz);
    }

    public Vec3 p(Vec3 v) {
        return p(v.x, v.y, v.z);
    }

    /** Local vector to global vector. */
    public Vec3 v(double x, double y, double z) {
        return new Vec3(x * xx + z * zx, y, x * xz + z * zz);
    }

    public Vec3 v(Vec3 v) {
        return v(v.x, v.y, v.z);
    }

    /** Global point to local point. */
    public Vec3 ip(double x, double y, double z) {
        return iv(x - origin.x, y - origin.y, z - origin.z);
    }

    public Vec3 ip(Vec3 v) {
        return ip(v.x, v.y, v.z);
    }

    /** Global vector to local vector (the rotation is orthonormal, so the inverse is the transpose). */
    public Vec3 iv(double x, double y, double z) {
        return new Vec3(x * xx + z * xz, y, x * zx + z * zz);
    }

    public Vec3 iv(Vec3 v) {
        return iv(v.x, v.y, v.z);
    }

    /** Block containing the local point. */
    public BlockPos blockPos(double x, double y, double z) {
        return BlockPos.containing(p(x, y, z));
    }

    /** Global axis-aligned box spanned by two local corner points. */
    public AABB box(Vec3 a, Vec3 b) {
        return new AABB(p(a), p(b));
    }

    /** Angle for {@code Axis.YP.rotationDegrees()} that maps local axes onto global ones. */
    public float turnDegrees() {
        return turnDegrees(facing);
    }

    public static float turnDegrees(Direction facing) {
        return 180f - facing.toYRot();
    }
}
