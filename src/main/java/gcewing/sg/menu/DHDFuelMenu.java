package gcewing.sg.menu;

import gcewing.sg.ModRegistry;
import gcewing.sg.block.entity.DHDBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DHDFuelMenu extends BaseMenu {

    public static final int guiWidth = 256;
    public static final int guiHeight = 208;
    static final int numFuelSlotColumns = 2;
    static final int fuelSlotsX = 174;
    static final int fuelSlotsY = 84;
    static final int playerSlotsX = 48;
    static final int playerSlotsY = 124;

    public final DHDBlockEntity te;

    public DHDFuelMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, (DHDBlockEntity) playerInv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public DHDFuelMenu(int id, Inventory playerInv, DHDBlockEntity te) {
        super(ModRegistry.DHD_FUEL_MENU.get(), id);
        this.te = te;
        for (int i = 0; i < DHDBlockEntity.numFuelSlots; i++) {
            int row = i / numFuelSlotColumns;
            int col = i % numFuelSlotColumns;
            addSlot(new Slot(te.inventory, DHDBlockEntity.firstFuelSlot + i, fuelSlotsX + col * 18, fuelSlotsY + row * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return DHDBlockEntity.isValidFuelItem(stack);
                }
            });
        }
        numBlockSlots = DHDBlockEntity.numFuelSlots;
        addPlayerSlots(playerInv, playerSlotsX, playerSlotsY);
    }

    @Override
    public boolean stillValid(Player player) {
        return te != null && te.inventory.stillValid(player);
    }
}
