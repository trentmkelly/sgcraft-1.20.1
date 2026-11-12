package gcewing.sg.worldgen;

import com.mojang.serialization.Codec;
import gcewing.sg.ModRegistry;
import gcewing.sg.SGConfig;
import gcewing.sg.block.DHDBlock;
import gcewing.sg.block.SGBaseBlock;
import gcewing.sg.block.entity.SGBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.structures.DesertPyramidPiece;

/**
 * Buried stargate chamber under some desert pyramids (port of FeatureUnderDesertPyramid).
 *
 * The original injected an extra structure piece into the pyramid. Here a feature runs after
 * structures have been placed in the pyramid's starting chunk and builds the chamber relative to
 * the pyramid piece. Since 1.20 pyramids have a cellar and a deeper trap room where the original
 * chamber sat, so the chamber is placed below them and reached by a ladder shaft from the
 * pyramid's entrance corridor.
 */
public class PyramidStargateFeature extends Feature<NoneFeatureConfiguration> {

    /** Pyramid-local position of the top of the access shaft (in the entrance corridor floor). */
    static final int shaftLocalX = 10, shaftLocalZ = 2;
    /** Pyramid-local y of the chamber floor (below the trap room, which reaches down to -14). */
    static final int chamberLocalY = -24;

    public PyramidStargateFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        if (!SGConfig.augmentStructures.get())
            return false;
        WorldGenLevel level = ctx.level();
        Structure pyramid = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(BuiltinStructures.DESERT_PYRAMID);
        if (pyramid == null)
            return false;
        StructureStart start = level.getChunk(ctx.origin()).getStartForStructure(pyramid);
        if (start == null || !start.isValid())
            return false;
        RandomSource random = ctx.random();
        if (random.nextInt(100) >= SGConfig.structureAugmentationChance.get())
            return false;
        for (StructurePiece piece : start.getPieces())
            if (piece instanceof DesertPyramidPiece p) {
                build(level, p);
                return true;
            }
        return false;
    }

    static BlockPos pyramidToWorld(StructurePiece piece, int x, int y, int z) {
        BoundingBox box = piece.getBoundingBox();
        Direction o = piece.getOrientation();
        int wx, wz;
        if (o == null) {
            wx = x;
            wz = z;
        } else {
            switch (o) {
                case NORTH -> { wx = box.minX() + x; wz = box.maxZ() - z; }
                case SOUTH -> { wx = box.minX() + x; wz = box.minZ() + z; }
                case WEST -> { wx = box.maxX() - z; wz = box.minZ() + x; }
                default -> { wx = box.minX() + z; wz = box.minZ() + x; }
            }
        }
        return new BlockPos(wx, box.minY() + y, wz);
    }

    void build(WorldGenLevel level, DesertPyramidPiece piece) {
        BlockPos shaftTop = pyramidToWorld(piece, shaftLocalX, 0, shaftLocalZ);
        int floorY = piece.getBoundingBox().minY() + chamberLocalY;
        // Chamber coordinates: room spans 0..10 in x and z, 0..7 in y; the shaft is at (9, 9).
        BlockPos c = new BlockPos(shaftTop.getX() - 9, floorY, shaftTop.getZ() - 9);

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState sandstone = Blocks.CUT_SANDSTONE.defaultBlockState();
        BlockState orange = Blocks.ORANGE_TERRACOTTA.defaultBlockState();
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH);

        // Room: hollow sandstone box with an orange band and floor cross, as in the original.
        for (int x = 0; x <= 10; x++)
            for (int y = 0; y <= 7; y++)
                for (int z = 0; z <= 10; z++) {
                    boolean shell = x == 0 || x == 10 || y == 0 || y == 7 || z == 0 || z == 10;
                    BlockState s = shell ? (y == 3 ? orange : sandstone) : air;
                    set(level, c.offset(x, y, z), s);
                }
        for (int i = 3; i <= 7; i++) {
            set(level, c.offset(i, 0, 5), orange);
            set(level, c.offset(5, 0, i), orange);
        }

        // Access shaft from the room's corner up to the pyramid's entrance corridor.
        int topY = shaftTop.getY();
        for (int y = 1; y <= topY - c.getY(); y++) {
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    if ((dx != 0 || dz != 0) && y > 7)
                        set(level, c.offset(9 + dx, y, 9 + dz), sandstone);
            set(level, c.offset(9, y, 9), y < topY - c.getY() || y <= 7 ? ladder : air);
        }
        set(level, shaftTop, air);
        set(level, shaftTop.above(), air);
        set(level, c.offset(9, topY - c.getY(), 9), ladder);

        // Stargate along x at z = 2, facing so that its front opens into the room.
        BlockState base = ModRegistry.STARGATE_BASE.get().defaultBlockState().setValue(SGBaseBlock.FACING, Direction.NORTH);
        BlockState ring = ModRegistry.STARGATE_RING.get().defaultBlockState();
        BlockState chevron = ModRegistry.STARGATE_CHEVRON.get().defaultBlockState();
        for (int i = -2; i <= 2; i++)
            for (int j = 0; j <= 4; j++) {
                BlockState s;
                if (i == 0 && j == 0)
                    s = base;
                else if (i == -2 || i == 2 || j == 0 || j == 4)
                    s = ((i + j + 1) & 1) == 1 ? chevron : ring;
                else
                    s = air;
                set(level, c.offset(5 + i, 1 + j, 2), s);
            }
        if (level.getBlockEntity(c.offset(5, 1, 2)) instanceof SGBaseBlockEntity te) {
            te.hasChevronUpgrade = true;
            te.setChanged();
        }
        set(level, c.offset(5, 1, 7),
            ModRegistry.STARGATE_CONTROLLER.get().defaultBlockState().setValue(DHDBlock.FACING, Direction.NORTH));
    }

    static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2);
    }
}
