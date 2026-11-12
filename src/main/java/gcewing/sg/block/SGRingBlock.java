package gcewing.sg.block;

import gcewing.sg.block.entity.SGBaseBlockEntity;
import gcewing.sg.block.entity.SGRingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Stargate ring segment. The original had one block with a ring/chevron variant; after the
 * flattening these are two separate blocks sharing this class.
 */
public class SGRingBlock extends Block implements EntityBlock, ISGBlock {

    public final boolean chevron;

    public SGRingBlock(Properties props, boolean chevron) {
        super(props);
        this.chevron = chevron;
    }

    /** Value used in the merge pattern of SGBaseBlock: 1 = ring, 2 = chevron. */
    public int patternType() {
        return chevron ? 2 : 1;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SGRingBlockEntity(pos, state);
    }

    static SGRingBlockEntity ringTE(BlockGetter world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof SGRingBlockEntity te ? te : null;
    }

    @Override
    public SGBaseBlockEntity getBaseTE(BlockGetter world, BlockPos pos) {
        SGRingBlockEntity rte = ringTE(world, pos);
        return rte != null ? rte.getBaseTE() : null;
    }

    @Override
    public boolean isMerged(BlockGetter world, BlockPos pos) {
        SGRingBlockEntity te = ringTE(world, pos);
        return te != null && te.isMerged;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        SGRingBlockEntity te = ringTE(world, pos);
        if (te != null && te.isMerged) {
            BlockState baseState = world.getBlockState(te.basePos);
            if (baseState.getBlock() instanceof SGBaseBlock)
                return baseState.use(world, player, hand, hit.withPosition(te.basePos));
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        return InteractionResult.PASS;
    }

    public void mergeWith(Level world, BlockPos pos, BlockPos basePos) {
        SGRingBlockEntity te = ringTE(world, pos);
        if (te != null) {
            te.isMerged = true;
            te.basePos = basePos;
            te.markChanged();
        }
    }

    public void unmergeFrom(Level world, BlockPos pos, BlockPos basePos) {
        SGRingBlockEntity te = ringTE(world, pos);
        if (te != null && te.isMerged && te.basePos.equals(basePos)) {
            te.isMerged = false;
            te.markChanged();
        }
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, world, pos, oldState, moving);
        if (!world.isClientSide && !oldState.is(state.getBlock()))
            updateBaseBlocks(world, pos, ringTE(world, pos));
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            SGRingBlockEntity te = ringTE(world, pos);
            if (!world.isClientSide && te != null && te.isMerged)
                updateBaseBlocks(world, pos, te);
        }
        super.onRemove(state, world, pos, newState, moving);
    }

    void updateBaseBlocks(Level world, BlockPos pos, SGRingBlockEntity te) {
        boolean merged = te != null && te.isMerged;
        BlockPos teBase = te != null ? te.basePos : null;
        for (int i = -2; i <= 2; i++)
            for (int j = -4; j <= 0; j++)
                for (int k = -2; k <= 2; k++) {
                    BlockPos bp = pos.offset(i, j, k);
                    if (world.getBlockState(bp).getBlock() instanceof SGBaseBlock base) {
                        if (!merged)
                            base.checkForMerge(world, bp);
                        else if (teBase.equals(bp))
                            base.unmerge(world, bp);
                    }
                }
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!ISGBlock.allowDestroy(state, level, pos, player))
            return false;
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }
}
