package gcewing.sg.cc;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import gcewing.sg.IComputerInterface;
import gcewing.sg.SGInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;

public class CCInterfaceBlockEntity extends BlockEntity implements IComputerInterface {

    static final Capability<IPeripheral> PERIPHERAL = CapabilityManager.get(new CapabilityToken<>() {});

    final Set<IComputerAccess> attachedComputers = ConcurrentHashMap.newKeySet();
    final SGInterface sgInterface = new SGInterface(this);
    final StargatePeripheral peripheral = new StargatePeripheral(this);
    LazyOptional<IPeripheral> peripheralCap = LazyOptional.of(() -> peripheral);

    public CCInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(CCIntegration.CC_INTERFACE_BE.get(), pos, state);
    }

    @Override
    public void postEvent(BlockEntity source, String name, Object... args) {
        for (IComputerAccess cpu : attachedComputers) {
            Object[] xargs = new Object[args.length + 1];
            xargs[0] = cpu.getAttachmentName();
            System.arraycopy(args, 0, xargs, 1, args.length);
            cpu.queueEvent(name, xargs);
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == PERIPHERAL)
            return peripheralCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        peripheralCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        peripheralCap = LazyOptional.of(() -> peripheral);
    }
}
