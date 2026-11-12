package gcewing.sg.menu;

import gcewing.sg.ModRegistry;
import gcewing.sg.block.entity.PowerUnitBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class PowerMenu extends BaseMenu {

    public final PowerUnitBlockEntity te;

    public PowerMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, (PowerUnitBlockEntity) playerInv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public PowerMenu(int id, Inventory playerInv, PowerUnitBlockEntity te) {
        super(ModRegistry.POWER_MENU.get(), id);
        this.te = te;
    }

    @Override
    public boolean stillValid(Player player) {
        return te != null && !te.isRemoved()
            && player.distanceToSqr(Vec3.atCenterOf(te.getBlockPos())) <= 64.0;
    }
}
