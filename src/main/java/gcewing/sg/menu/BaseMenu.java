package gcewing.sg.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Container with some block slots followed by the player's inventory (Greg's BaseContainer). */
public abstract class BaseMenu extends AbstractContainerMenu {

    protected int numBlockSlots;

    protected BaseMenu(MenuType<?> type, int id) {
        super(type, id);
    }

    protected void addPlayerSlots(Inventory inventory, int x, int y) {
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                addSlot(new Slot(inventory, col + row * 9 + 9, x + col * 18, y + row * 18));
        for (int col = 0; col < 9; ++col)
            addSlot(new Slot(inventory, col, x + col * 18, y + 58));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < numBlockSlots) {
            if (!moveItemStackTo(stack, numBlockSlots, slots.size(), true))
                return ItemStack.EMPTY;
        } else {
            if (numBlockSlots == 0 || !moveItemStackTo(stack, 0, numBlockSlots, false))
                return ItemStack.EMPTY;
        }
        if (stack.isEmpty())
            slot.set(ItemStack.EMPTY);
        else
            slot.setChanged();
        return original;
    }
}
