package gcewing.sg.item;

import gcewing.sg.block.ISGBlock;
import gcewing.sg.block.entity.SGBaseBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Chevron and iris upgrades: used on any block of a stargate to install them. */
public class SGUpgradeItem extends Item {

    public enum Kind { CHEVRON, IRIS }

    final Kind kind;

    public SGUpgradeItem(Properties props, Kind kind) {
        super(props);
        this.kind = kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        if (world.getBlockState(ctx.getClickedPos()).getBlock() instanceof ISGBlock block) {
            SGBaseBlockEntity te = block.getBaseTE(world, ctx.getClickedPos());
            if (te != null) {
                boolean applied = kind == Kind.CHEVRON
                    ? te.applyChevronUpgrade(ctx.getItemInHand(), ctx.getPlayer())
                    : te.applyIrisUpgrade(ctx.getItemInHand(), ctx.getPlayer());
                return applied ? InteractionResult.sidedSuccess(world.isClientSide) : InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }
}
