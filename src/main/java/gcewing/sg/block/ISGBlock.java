package gcewing.sg.block;

import gcewing.sg.block.entity.SGBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Common behaviour of the blocks making up a stargate (ring, chevron and base blocks). */
public interface ISGBlock {

    SGBaseBlockEntity getBaseTE(BlockGetter world, BlockPos pos);

    boolean isMerged(BlockGetter world, BlockPos pos);

    /** Creative players can't break a connected gate (original SGBlock.removedByPlayer). */
    static boolean allowDestroy(BlockState state, Level level, BlockPos pos, Player player) {
        if (player.getAbilities().instabuild && state.getBlock() instanceof ISGBlock b) {
            SGBaseBlockEntity bte = b.getBaseTE(level, pos);
            if (bte != null && bte.isConnected()) {
                if (level.isClientSide)
                    player.displayClientMessage(Component.translatable("message.sgcraft.disconnect_before_breaking"), false);
                return false;
            }
        }
        return true;
    }
}
