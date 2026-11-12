package gcewing.sg.block.entity;

import gcewing.sg.ISGEnergySource;
import gcewing.sg.ModRegistry;
import gcewing.sg.menu.PowerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

/** Buffers Forge Energy for a stargate (port of the RF stargate power unit, same numbers). */
public class PowerUnitBlockEntity extends SyncedBlockEntity implements ISGEnergySource, MenuProvider {

    public static final double maxEnergyBuffer = 4000000;
    public static final double fePerSGEnergyUnit = 80.0;

    public double energyBuffer;
    boolean dirtyForClient;

    final IEnergyStorage storage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int e = (int) Math.max(0, Math.min(maxEnergyBuffer - energyBuffer, maxReceive));
            if (!simulate && e > 0)
                addEnergy(e);
            return e;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int e = (int) Math.max(0, Math.min(energyBuffer, maxExtract));
            if (!simulate && e > 0)
                addEnergy(-e);
            return e;
        }

        @Override
        public int getEnergyStored() {
            return (int) energyBuffer;
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) maxEnergyBuffer;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };
    LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> storage);

    public PowerUnitBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.POWER_UNIT_BE.get(), pos, state);
    }

    void addEnergy(double e) {
        energyBuffer += e;
        setChanged();
        dirtyForClient = true;
    }

    public void serverTick() {
        // Energy changes every tick while cables feed us; sync clients at most twice a second.
        if (dirtyForClient && level.getGameTime() % 10 == 0) {
            dirtyForClient = false;
            markChanged();
        }
    }

    @Override
    public double availableEnergy() {
        return energyBuffer / fePerSGEnergyUnit;
    }

    @Override
    public double drawEnergy(double request) {
        double available = energyBuffer / fePerSGEnergyUnit;
        double supply = Math.min(request, available);
        addEnergy(-supply * fePerSGEnergyUnit);
        return supply;
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        energyBuffer = nbt.getDouble("energyBuffer");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putDouble("energyBuffer", energyBuffer);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ENERGY)
            return energyCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        energyCap = LazyOptional.of(() -> storage);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.sgcraft.power_unit");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new PowerMenu(id, playerInv, this);
    }
}
