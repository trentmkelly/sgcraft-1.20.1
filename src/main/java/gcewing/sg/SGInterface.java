package gcewing.sg;

import gcewing.sg.block.entity.SGBaseBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Computer-facing stargate operations shared by computer interface blocks (port of
 * SGInterfaceTE). The interface block must sit directly below a block of the gate's bottom row.
 * Errors are reported by throwing IllegalArgumentException with a user-facing message.
 */
public class SGInterface {

    public interface Call<T> {
        T call();
    }

    public record StargateState(String state, int chevrons, String direction) {
    }

    final BlockEntity te;

    public SGInterface(BlockEntity te) {
        this.te = te;
    }

    public SGBaseBlockEntity getBaseTE() {
        if (te.getLevel() == null)
            return null;
        return SGBaseBlockEntity.get(te.getLevel(), te.getBlockPos().above());
    }

    public SGBaseBlockEntity requireBaseTE() {
        SGBaseBlockEntity bte = getBaseTE();
        if (bte != null && bte.isMerged)
            return bte;
        throw new IllegalArgumentException("No stargate connected to interface");
    }

    public SGBaseBlockEntity requireIrisTE() {
        SGBaseBlockEntity bte = requireBaseTE();
        if (bte.hasIrisUpgrade)
            return bte;
        throw new IllegalArgumentException("No iris fitted to stargate");
    }

    static String directionDescription(SGBaseBlockEntity bte) {
        if (bte.isConnected())
            return bte.isInitiator ? "Outgoing" : "Incoming";
        return "";
    }

    public StargateState stargateState() {
        SGBaseBlockEntity bte = getBaseTE();
        if (bte != null)
            return new StargateState(bte.sgStateDescription(), bte.numEngagedChevrons, directionDescription(bte));
        return new StargateState("Offline", 0, "");
    }

    public double energyAvailable() {
        SGBaseBlockEntity bte = getBaseTE();
        return bte != null ? bte.availableEnergy() : 0;
    }

    public double energyToDial(String address) {
        SGBaseBlockEntity bte = requireBaseTE();
        try {
            address = SGAddressing.normalizeAddress(address);
            SGBaseBlockEntity dte = SGAddressing.findAddressedStargate(address, bte.getLevel());
            if (dte == null)
                throw new IllegalArgumentException("No stargate at address " + address);
            double distanceFactor = SGBaseBlockEntity.distanceFactorForCoordDifference(bte, dte);
            return SGConfig.energyToOpen() * distanceFactor;
        } catch (SGAddressing.AddressingError e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public String localAddress() {
        SGBaseBlockEntity bte = getBaseTE();
        try {
            return bte != null ? bte.getHomeAddress() : "";
        } catch (SGAddressing.AddressingError e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public String remoteAddress() {
        SGBaseBlockEntity bte = requireBaseTE();
        try {
            return bte.connectedLocation != null ? SGAddressing.addressForLocation(bte.connectedLocation) : "";
        } catch (SGAddressing.AddressingError e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public void dial(String address) {
        SGBaseBlockEntity bte = requireBaseTE();
        String error = bte.connect(SGAddressing.normalizeAddress(address), null);
        if (error != null)
            throw new IllegalArgumentException(error);
    }

    public void disconnect() {
        SGBaseBlockEntity bte = requireBaseTE();
        String error = bte.attemptToDisconnect(null);
        if (error != null)
            throw new IllegalArgumentException(error);
    }

    public String irisState() {
        SGBaseBlockEntity bte = getBaseTE();
        if (bte != null && bte.hasIrisUpgrade)
            return bte.irisStateDescription();
        return "Offline";
    }

    public void openIris() {
        requireIrisTE().openIris();
    }

    public void closeIris() {
        requireIrisTE().closeIris();
    }

    public void sendMessage(Object[] args) {
        SGBaseBlockEntity bte = requireBaseTE();
        String error = bte.sendMessage(args);
        if (error != null)
            throw new IllegalArgumentException(error);
    }
}
