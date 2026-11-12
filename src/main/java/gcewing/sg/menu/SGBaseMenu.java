package gcewing.sg.menu;

import gcewing.sg.ModRegistry;
import gcewing.sg.block.entity.SGBaseBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SGBaseMenu extends BaseMenu {

    public static final int guiWidth = 256;
    public static final int guiHeight = 208;

    public final SGBaseBlockEntity te;

    public SGBaseMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, (SGBaseBlockEntity) playerInv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public SGBaseMenu(int id, Inventory playerInv, SGBaseBlockEntity te) {
        super(ModRegistry.SG_BASE_MENU.get(), id);
        this.te = te;
        for (int i = 0; i < SGBaseBlockEntity.numCamouflageSlots; i++)
            addSlot(new Slot(te.inventory, SGBaseBlockEntity.firstCamouflageSlot + i, 48 + i * 18, 104) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return SGBaseBlockEntity.isValidCamouflage(stack);
                }
            });
        numBlockSlots = SGBaseBlockEntity.numCamouflageSlots;
        addPlayerSlots(playerInv, (guiWidth - 160) / 2, guiHeight - 82);
    }

    @Override
    public boolean stillValid(Player player) {
        return te != null && te.stillValid(player);
    }
}
