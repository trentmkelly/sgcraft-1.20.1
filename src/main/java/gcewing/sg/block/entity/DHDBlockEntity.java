package gcewing.sg.block.entity;

import gcewing.sg.ISGEnergySource;
import gcewing.sg.ModRegistry;
import gcewing.sg.SGConfig;
import gcewing.sg.block.DHDBlock;
import gcewing.sg.menu.DHDFuelMenu;
import gcewing.sg.util.GateTransform;
import gcewing.sg.util.InventoryNBT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DHDBlockEntity extends SyncedBlockEntity implements ISGEnergySource, MenuProvider {

    public static final int firstFuelSlot = 0;
    public static final int numFuelSlots = 4;

    public boolean isLinkedToStargate;
    public BlockPos linkedPos = BlockPos.ZERO;
    public String enteredAddress = "";
    public double energyInBuffer;

    public final SimpleContainer inventory = new SimpleContainer(numFuelSlots) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return isValidFuelItem(stack);
        }

        @Override
        public boolean stillValid(Player player) {
            return !isRemoved() && player.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= 64.0;
        }
    };

    public DHDBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.DHD_BE.get(), pos, state);
        inventory.addListener(c -> {
            if (!InventoryNBT.loading)
                setChanged();
        });
    }

    public static double maxEnergyBuffer() {
        return SGConfig.energyPerFuelItem.get();
    }

    public void setEnteredAddress(String address) {
        enteredAddress = address;
        markChanged();
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(0.5);
    }

    public Direction facing() {
        BlockState bs = getBlockState();
        return bs.hasProperty(DHDBlock.FACING) ? bs.getValue(DHDBlock.FACING) : Direction.NORTH;
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        isLinkedToStargate = nbt.getBoolean("isLinkedToStargate");
        energyInBuffer = nbt.getDouble("energyInBuffer");
        linkedPos = new BlockPos(nbt.getInt("linkedX"), nbt.getInt("linkedY"), nbt.getInt("linkedZ"));
        enteredAddress = nbt.getString("enteredAddress");
        InventoryNBT.load(inventory, nbt);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putBoolean("isLinkedToStargate", isLinkedToStargate);
        nbt.putDouble("energyInBuffer", energyInBuffer);
        nbt.putInt("linkedX", linkedPos.getX());
        nbt.putInt("linkedY", linkedPos.getY());
        nbt.putInt("linkedZ", linkedPos.getZ());
        nbt.putString("enteredAddress", enteredAddress);
        InventoryNBT.save(inventory, nbt);
    }

    public SGBaseBlockEntity getLinkedStargateTE() {
        if (isLinkedToStargate && level != null && level.isLoaded(linkedPos)) {
            if (level.getBlockEntity(linkedPos) instanceof SGBaseBlockEntity gte)
                return gte;
        }
        return null;
    }

    public void checkForLink() {
        if (!isLinkedToStargate) {
            int rx = SGConfig.linkRangeX.get(), ry = SGConfig.linkRangeY.get(), rz = SGConfig.linkRangeZ.get();
            GateTransform t = GateTransform.blockCenter(worldPosition, facing());
            for (int i = -rx; i <= rx; i++)
                for (int j = -ry; j <= ry; j++)
                    for (int k = 1; k <= rz; k++) {
                        BlockPos bp = t.blockPos(i, j, -k);
                        if (level.isLoaded(bp) && level.getBlockEntity(bp) instanceof SGBaseBlockEntity gte)
                            if (linkToStargate(gte))
                                return;
                    }
        }
        updateButtonState();
    }

    boolean linkToStargate(SGBaseBlockEntity gte) {
        if (!isLinkedToStargate && !gte.isLinkedToController && gte.isMerged) {
            linkedPos = gte.getBlockPos();
            isLinkedToStargate = true;
            markChanged();
            gte.linkedPos = worldPosition;
            gte.isLinkedToController = true;
            gte.markChanged();
            updateButtonState();
            return true;
        }
        return false;
    }

    public void clearLinkToStargate() {
        isLinkedToStargate = false;
        markChanged();
        updateButtonState();
    }

    /** Light up the DHD's centre button to match the linked stargate (was done by its TE renderer). */
    public void updateButtonState() {
        if (level == null || level.isClientSide)
            return;
        BlockState bs = getBlockState();
        if (!bs.hasProperty(DHDBlock.BUTTON))
            return;
        SGBaseBlockEntity gte = getLinkedStargateTE();
        DHDBlock.Button b = gte == null || !gte.isMerged ? DHDBlock.Button.OFF
            : gte.isActive() ? DHDBlock.Button.LIT : DHDBlock.Button.IDLE;
        if (bs.getValue(DHDBlock.BUTTON) != b)
            level.setBlock(worldPosition, bs.setValue(DHDBlock.BUTTON, b), Block.UPDATE_CLIENTS);
    }

    @Override
    public double availableEnergy() {
        double energy = energyInBuffer;
        for (int i = 0; i < numFuelSlots; i++) {
            ItemStack stack = fuelStackInSlot(i);
            if (!stack.isEmpty())
                energy += stack.getCount() * SGConfig.energyPerFuelItem.get();
        }
        return energy;
    }

    @Override
    public double drawEnergy(double amount) {
        double energyDrawn = 0;
        while (energyDrawn < amount) {
            if (energyInBuffer <= 0) {
                if (!useFuelItem())
                    break;
            }
            double e = Math.min(amount - energyDrawn, energyInBuffer);
            energyDrawn += e;
            energyInBuffer -= e;
        }
        markChanged();
        return energyDrawn;
    }

    boolean useFuelItem() {
        for (int i = numFuelSlots - 1; i >= 0; i--) {
            ItemStack stack = fuelStackInSlot(i);
            if (!stack.isEmpty()) {
                inventory.removeItem(firstFuelSlot + i, 1);
                energyInBuffer += SGConfig.energyPerFuelItem.get();
                return true;
            }
        }
        return false;
    }

    ItemStack fuelStackInSlot(int i) {
        ItemStack stack = inventory.getItem(firstFuelSlot + i);
        return isValidFuelItem(stack) ? stack : ItemStack.EMPTY;
    }

    public static boolean isValidFuelItem(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModRegistry.NAQUADAH.get());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.sgcraft.stargate_controller");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new DHDFuelMenu(id, playerInv, this);
    }
}
