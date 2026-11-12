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
import net.minecraft.world.level.ChunkPos;
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
 * The original injected an extra structure piece into the pyramid. Here a feature runs in each
 * chunk the pyramid touches, after structures have been placed there, and builds that chunk's
 * share of the chamber relative to the pyramid piece. Since 1.20 pyramids have a cellar and a deeper trap room where the original
 * chamber sat, so the chamber is placed below them and reached by a ladder shaft from the
 * floor of the pyramid's main hall.
 */
public class PyramidStargateFeature extends Feature<NoneFeatureConfiguration> {

    /** Pyramid-local position of the top of the access shaft (main hall floor, beside a pillar). */
    static final int shaftLocalX = 6, shaftLocalZ = 6;
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
        ChunkPos here = new ChunkPos(ctx.origin());
        // Every chunk touched by a pyramid builds its own part of the chamber, after that chunk's
        // part of the pyramid has been placed, so the pyramid can't overwrite the chamber.
        boolean placed = false;
        for (long ref : level.getChunk(here.x, here.z).getReferencesForStructure(pyramid)) {
            ChunkPos startPos = new ChunkPos(ref);
            StructureStart start = level.getChunk(startPos.x, startPos.z).getStartForStructure(pyramid);
            if (start == null || !start.isValid())
                continue;
            // Roll once per pyramid (not per chunk) so all its chunks agree.
            RandomSource random = RandomSource.create(level.getSeed() ^ (ref * 0x5DEECE66DL) ^ 0x5347L);
            if (random.nextInt(100) >= SGConfig.structureAugmentationChance.get())
                continue;
            for (StructurePiece piece : start.getPieces())
                if (piece instanceof DesertPyramidPiece p) {
                    build(level, p, here);
                    placed = true;
                    break;
                }
        }
        return placed;
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

    void build(WorldGenLevel level, DesertPyramidPiece piece, ChunkPos chunk) {
        Clip clip = new Clip(level, chunk);
        BoundingBox box = piece.getBoundingBox();
        // Chamber coordinates: an 11x11 room under the middle of the pyramid's footprint (so every
        // chunk it touches belongs to the pyramid), 0..7 high.
        BlockPos c = new BlockPos(box.minX() + 5, box.minY() + chamberLocalY, box.minZ() + 5);
        BlockPos shaftTop = pyramidToWorld(piece, shaftLocalX, 0, shaftLocalZ);
        int sx = shaftTop.getX() - c.getX(), sz = shaftTop.getZ() - c.getZ();
        // Ladder hangs on whichever room wall is nearest the shaft.
        Direction ladderFacing = sz <= 5 ? Direction.SOUTH : Direction.NORTH;

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState sandstone = Blocks.CUT_SANDSTONE.defaultBlockState();
        BlockState orange = Blocks.ORANGE_TERRACOTTA.defaultBlockState();
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, ladderFacing);
        int wallDz = ladderFacing == Direction.SOUTH ? -1 : 1;

        // Room: hollow sandstone box with an orange band and floor cross, as in the original.
        for (int x = 0; x <= 10; x++)
            for (int y = 0; y <= 7; y++)
                for (int z = 0; z <= 10; z++) {
                    boolean shell = x == 0 || x == 10 || y == 0 || y == 7 || z == 0 || z == 10;
                    BlockState s = shell ? (y == 3 ? orange : sandstone) : air;
                    clip.set(c.offset(x, y, z), s);
                }
        for (int i = 3; i <= 7; i++) {
            clip.set(c.offset(i, 0, 5), orange);
            clip.set(c.offset(5, 0, i), orange);
        }

        // Access shaft from the room's corner up to the floor of the pyramid's main hall.
        int top = shaftTop.getY() - c.getY();
        for (int y = 1; y <= top; y++) {
            if (y > 7)
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++)
                        if (dx != 0 || dz != 0)
                            clip.set(c.offset(sx + dx, y, sz + dz), sandstone);
            if (y <= 7)
                clip.set(c.offset(sx, y, sz + wallDz), y == 3 ? orange : sandstone);
            clip.set(c.offset(sx, y, sz), ladder);
        }
        clip.set(shaftTop.above(), air);
        clip.set(shaftTop.above(2), air);

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
                clip.set(c.offset(5 + i, 1 + j, 2), s);
            }
        if (clip.contains(c.offset(5, 1, 2)) && level.getBlockEntity(c.offset(5, 1, 2)) instanceof SGBaseBlockEntity te) {
            te.hasChevronUpgrade = true;
            te.setChanged();
        }
        clip.set(c.offset(5, 1, 7),
            ModRegistry.STARGATE_CONTROLLER.get().defaultBlockState().setValue(DHDBlock.FACING, Direction.NORTH));
    }

    /** Writes only inside one chunk. */
    record Clip(WorldGenLevel level, ChunkPos chunk) {
        boolean contains(BlockPos pos) {
            return (pos.getX() >> 4) == chunk.x && (pos.getZ() >> 4) == chunk.z;
        }

        void set(BlockPos pos, BlockState state) {
            if (contains(pos))
                level.setBlock(pos, state, 2);
        }
    }
}
