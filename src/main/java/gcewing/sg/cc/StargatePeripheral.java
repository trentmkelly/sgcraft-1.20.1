package gcewing.sg.cc;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import gcewing.sg.SGInterface;

/** Peripheral type "stargate" with the same methods as the original ComputerCraft interface. */
public class StargatePeripheral implements IPeripheral {

    final CCInterfaceBlockEntity te;

    StargatePeripheral(CCInterfaceBlockEntity te) {
        this.te = te;
    }

    SGInterface ci() {
        return te.sgInterface;
    }

    @Override
    public String getType() {
        return "stargate";
    }

    @Override
    public boolean equals(IPeripheral other) {
        return other instanceof StargatePeripheral p && p.te == te;
    }

    @Override
    public Object getTarget() {
        return te;
    }

    @Override
    public void attach(IComputerAccess computer) {
        te.attachedComputers.add(computer);
    }

    @Override
    public void detach(IComputerAccess computer) {
        te.attachedComputers.remove(computer);
    }

    static <T> T call(SGInterface.Call<T> c) throws LuaException {
        try {
            return c.call();
        } catch (IllegalArgumentException e) {
            throw new LuaException(e.getMessage());
        }
    }

    @LuaFunction(mainThread = true)
    public final MethodResult stargateState() throws LuaException {
        SGInterface.StargateState s = call(() -> ci().stargateState());
        return MethodResult.of(s.state(), s.chevrons(), s.direction());
    }

    @LuaFunction(mainThread = true)
    public final double energyAvailable() throws LuaException {
        return call(() -> ci().energyAvailable());
    }

    @LuaFunction(mainThread = true)
    public final double energyToDial(String address) throws LuaException {
        return call(() -> ci().energyToDial(address));
    }

    @LuaFunction(mainThread = true)
    public final String localAddress() throws LuaException {
        return call(() -> ci().localAddress());
    }

    @LuaFunction(mainThread = true)
    public final String remoteAddress() throws LuaException {
        return call(() -> ci().remoteAddress());
    }

    @LuaFunction(mainThread = true)
    public final void dial(String address) throws LuaException {
        call(() -> {
            ci().dial(address);
            return null;
        });
    }

    @LuaFunction(mainThread = true)
    public final void disconnect() throws LuaException {
        call(() -> {
            ci().disconnect();
            return null;
        });
    }

    @LuaFunction(mainThread = true)
    public final String irisState() throws LuaException {
        return call(() -> ci().irisState());
    }

    @LuaFunction(mainThread = true)
    public final void openIris() throws LuaException {
        call(() -> {
            ci().openIris();
            return null;
        });
    }

    @LuaFunction(mainThread = true)
    public final void closeIris() throws LuaException {
        call(() -> {
            ci().closeIris();
            return null;
        });
    }

    @LuaFunction(mainThread = true)
    public final void sendMessage(IArguments args) throws LuaException {
        Object[] values = args.getAll();
        call(() -> {
            ci().sendMessage(values);
            return null;
        });
    }
}
