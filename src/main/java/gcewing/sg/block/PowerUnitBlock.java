package gcewing.sg.block;

import gcewing.sg.ModRegistry;
import gcewing.sg.block.entity.PowerUnitBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/**
 * Stargate power unit. Replaces the original RF and IC2 power units with a single unit that
 * accepts Forge Energy (FE), which is what RF became.
 */
public class PowerUnitBlock extends Block implements EntityBlock {

    public PowerUnitBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PowerUnitBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == ModRegistry.POWER_UNIT_BE.get()
            ? (l, p, s, te) -> ((PowerUnitBlockEntity) te).serverTick() : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && world.getBlockEntity(pos) instanceof PowerUnitBlockEntity te)
            NetworkHooks.openScreen(sp, te, pos);
        return InteractionResult.sidedSuccess(world.isClientSide);
    }
}
