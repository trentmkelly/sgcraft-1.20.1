package gcewing.sg.util;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/** Slot-preserving save/load for SimpleContainer (its own fromTag compacts the items). */
public final class InventoryNBT {

    /** True while a container is being filled from NBT, so change listeners can ignore it. */
    public static boolean loading;

    private InventoryNBT() {
    }

    public static void save(SimpleContainer inv, CompoundTag nbt) {
        NonNullList<ItemStack> items = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++)
            items.set(i, inv.getItem(i));
        ContainerHelper.saveAllItems(nbt, items, true);
    }

    public static void load(SimpleContainer inv, CompoundTag nbt) {
        NonNullList<ItemStack> items = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbt, items);
        loading = true;
        try {
            for (int i = 0; i < items.size(); i++)
                inv.setItem(i, items.get(i));
        } finally {
            loading = false;
        }
    }
}
