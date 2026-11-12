package gcewing.sg.worldgen;

import com.mojang.serialization.Codec;
import gcewing.sg.ModRegistry;
import gcewing.sg.SGConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Port of NaquadahOreWorldGen: nodes of naquadah ore under lava, plus a few isolated nodes deep
 * underground. Runs once per chunk (the placed feature has no placement modifiers, so the origin
 * is the chunk's corner). Heights that were absolute in the original are measured from the
 * bottom of the world, since worlds now extend below y=0.
 */
public class NaquadahOreFeature extends Feature<NoneFeatureConfiguration> {

    public NaquadahOreFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        if (!SGConfig.enableNaquadahOre.get())
            return false;
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        int minY = level.getMinBuildHeight();
        BlockState ore = ModRegistry.NAQUADAH_ORE.get().defaultBlockState();
        boolean placed = false;
        if (random.nextInt(SGConfig.genUnderLavaOdds.get()) == 0) {
            int n = random.nextInt(SGConfig.maxNodesUnderLava.get()) + 1;
            for (int i = 0; i < n; i++) {
                int x = random.nextInt(16);
                int z = random.nextInt(16);
                for (int y = minY + 1; y < 64; y++) {
                    BlockPos p = new BlockPos(x0 + x, y, z0 + z);
                    if (isSolidBlock(level, p) && level.getBlockState(p.above()).is(Blocks.LAVA))
                        placed |= generateNode(level, random, ore, x0, z0, x, y, z, 3, 1, 3);
                }
            }
        }
        if (random.nextInt(SGConfig.genIsolatedOdds.get()) == 0) {
            int n = random.nextInt(SGConfig.maxIsolatedNodes.get()) + 1;
            for (int i = 0; i < n; i++) {
                int x = random.nextInt(16);
                int y = minY + random.nextInt(16) + 16;
                int z = random.nextInt(16);
                if (isSolidBlock(level, new BlockPos(x0 + x, y, z0 + z)))
                    placed |= generateNode(level, random, ore, x0, z0, x, y, z, 2, 2, 2);
            }
        }
        return placed;
    }

    static boolean isSolidBlock(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isSolid() && !state.hasBlockEntity() && state.getDestroySpeed(level, pos) >= 0;
    }

    static boolean generateNode(WorldGenLevel level, RandomSource random, BlockState ore,
                                int x0, int z0, int x, int y, int z, int sx, int sy, int sz) {
        int dx = random.nextInt(sx);
        int dy = random.nextInt(sy);
        int dz = random.nextInt(sz);
        int h = level.getMaxBuildHeight();
        boolean placed = false;
        for (int i = x; i <= x + dx; i++)
            for (int j = y; j <= y + dy; j++)
                for (int k = z; k <= z + dz; k++)
                    if (i < 16 && j < h && k < 16) {
                        BlockPos pos = new BlockPos(x0 + i, j, z0 + k);
                        if (isSolidBlock(level, pos)) {
                            level.setBlock(pos, ore, 2);
                            placed = true;
                        }
                    }
        return placed;
    }
}
