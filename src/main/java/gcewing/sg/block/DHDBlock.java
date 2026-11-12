package gcewing.sg.block;

import gcewing.sg.block.entity.DHDBlockEntity;
import gcewing.sg.block.entity.SGBaseBlockEntity;
import gcewing.sg.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkHooks;

/** Stargate controller (DHD). */
public class DHDBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Button> BUTTON = EnumProperty.create("button", Button.class);

    /** Appearance of the big orange button: no gate linked, gate idle, gate active. */
    public enum Button implements StringRepresentable {
        OFF, IDLE, LIT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public DHDBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BUTTON, Button.OFF));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BUTTON);
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

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DHDBlockEntity(pos, state);
    }

    static DHDBlockEntity te(BlockGetter world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof DHDBlockEntity te ? te : null;
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, world, pos, oldState, moving);
        if (!world.isClientSide && !oldState.is(state.getBlock())) {
            DHDBlockEntity te = te(world, pos);
            if (te != null)
                te.checkForLink();
        }
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (!world.isClientSide) {
            DHDBlockEntity te = te(world, pos);
            if (te != null)
                te.checkForLink();
        }
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !world.isClientSide) {
            DHDBlockEntity cte = te(world, pos);
            if (cte != null) {
                Containers.dropContents(world, pos, cte.inventory);
                if (cte.isLinkedToStargate) {
                    SGBaseBlockEntity gte = cte.getLinkedStargateTE();
                    if (gte != null)
                        gte.clearLinkToController();
                }
            }
        }
        super.onRemove(state, world, pos, newState, moving);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        boolean top = hit.getLocation().y - pos.getY() > 0.5;
        if (top) {
            if (world.isClientSide)
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openDHDScreen(pos));
        } else if (player instanceof ServerPlayer sp) {
            DHDBlockEntity te = te(world, pos);
            if (te != null)
                NetworkHooks.openScreen(sp, te, pos);
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }
}
