package gcewing.sg.block;

import gcewing.sg.ModRegistry;
import gcewing.sg.SGConfig;
import gcewing.sg.SGState;
import gcewing.sg.block.entity.SGBaseBlockEntity;
import gcewing.sg.util.GateTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class SGBaseBlock extends Block implements EntityBlock, ISGBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** Ring layout seen from the front, top row first. 1 = ring, 2 = chevron, 0 = anything. */
    static final int[][] pattern = {
        {2, 1, 2, 1, 2},
        {1, 0, 0, 0, 1},
        {2, 0, 0, 0, 2},
        {1, 0, 0, 0, 1},
        {2, 1, 0, 1, 2},
    };

    public SGBaseBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public static GateTransform transform(BlockState state, BlockPos pos) {
        return GateTransform.blockCenter(pos, state.getValue(FACING));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SGBaseBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModRegistry.BASE_BE.get() ? (l, p, s, te) -> ((SGBaseBlockEntity) te).tick() : null;
    }

    @Override
    public SGBaseBlockEntity getBaseTE(BlockGetter world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof SGBaseBlockEntity te ? te : null;
    }

    @Override
    public boolean isMerged(BlockGetter world, BlockPos pos) {
        SGBaseBlockEntity te = getBaseTE(world, pos);
        return te != null && te.isMerged;
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, world, pos, oldState, moving);
        if (!world.isClientSide && !oldState.is(state.getBlock()))
            checkForMerge(world, pos);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        SGBaseBlockEntity te = getBaseTE(world, pos);
        if (te != null && te.isMerged) {
            if (player instanceof ServerPlayer sp)
                NetworkHooks.openScreen(sp, te, pos);
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, world, pos, block, fromPos, moving);
        SGBaseBlockEntity te = getBaseTE(world, pos);
        if (te != null)
            te.onNeighborBlockChange();
    }

    public void checkForMerge(Level world, BlockPos pos) {
        if (isMerged(world, pos))
            return;
        BlockState state = world.getBlockState(pos);
        if (!state.is(this))
            return;
        GateTransform t = transform(state, pos);
        for (int i = -2; i <= 2; i++)
            for (int j = 0; j <= 4; j++)
                if (!(i == 0 && j == 0)) {
                    BlockPos rp = t.blockPos(i, j, 0);
                    int type = getRingBlockType(world, rp);
                    int pat = pattern[4 - j][2 + i];
                    if (pat != 0 && type != pat)
                        return;
                }
        SGBaseBlockEntity te = getBaseTE(world, pos);
        if (te == null)
            return;
        te.setMerged(true);
        for (int i = -2; i <= 2; i++)
            for (int j = 0; j <= 4; j++)
                if (!(i == 0 && j == 0)) {
                    BlockPos rp = t.blockPos(i, j, 0);
                    if (world.getBlockState(rp).getBlock() instanceof SGRingBlock ring)
                        ring.mergeWith(world, rp, pos);
                }
        te.checkForLink();
    }

    int getRingBlockType(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir())
            return 0;
        if (state.getBlock() instanceof SGRingBlock ring && !ring.isMerged(world, pos))
            return ring.patternType();
        return -1;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !world.isClientSide) {
            SGBaseBlockEntity te = getBaseTE(world, pos);
            unmerge(world, pos);
            if (te != null) {
                if (te.hasChevronUpgrade)
                    Block.popResource(world, pos, new ItemStack(ModRegistry.SG_CHEVRON_UPGRADE.get()));
                if (te.hasIrisUpgrade)
                    Block.popResource(world, pos, new ItemStack(ModRegistry.SG_IRIS_UPGRADE.get()));
                Containers.dropContents(world, pos, te.inventory);
                te.onRemoved();
            }
        }
        super.onRemove(state, world, pos, newState, moving);
    }

    public void unmerge(Level world, BlockPos pos) {
        SGBaseBlockEntity te = getBaseTE(world, pos);
        boolean goBang = false;
        if (te != null) {
            if (te.isMerged && te.state == SGState.Connected) {
                te.state = SGState.Idle;
                goBang = true;
            }
            te.disconnect();
            te.unlinkFromController();
            te.setMerged(false);
            unmergeRing(world, pos);
        }
        int radius = SGConfig.explosionRadius.get();
        if (goBang && radius > 0 && world instanceof ServerLevel sl) {
            Vec3 p = Vec3.atLowerCornerOf(pos).add(0.5, 2.5, 0.5);
            // Deferred so the explosion doesn't break blocks while this one is still being removed.
            sl.getServer().execute(() -> sl.explode(null, p.x, p.y, p.z, radius, SGConfig.explosionFlame.get(),
                SGConfig.explosionSmoke.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE));
        }
    }

    void unmergeRing(Level world, BlockPos pos) {
        for (int i = -2; i <= 2; i++)
            for (int j = 0; j <= 4; j++)
                for (int k = -2; k <= 2; k++) {
                    BlockPos rp = pos.offset(i, j, k);
                    if (world.getBlockState(rp).getBlock() instanceof SGRingBlock ring)
                        ring.unmergeFrom(world, rp, pos);
                }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        SGBaseBlockEntity te = getBaseTE(world, pos);
        return (te != null && te.state != SGState.Idle) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        return getSignal(state, world, pos, side);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!ISGBlock.allowDestroy(state, level, pos, player))
            return false;
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }
}
