//------------------------------------------------------------------------------------------------
//
//   SG Craft - Stargate base tile entity
//
//------------------------------------------------------------------------------------------------

package gcewing.sg.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import gcewing.sg.IComputerInterface;
import gcewing.sg.ISGEnergySource;
import gcewing.sg.IrisState;
import gcewing.sg.ModRegistry;
import gcewing.sg.SGAddressing;
import gcewing.sg.SGChunkLoading;
import gcewing.sg.SGConfig;
import gcewing.sg.SGDamage;
import gcewing.sg.SGLocation;
import gcewing.sg.SGState;
import gcewing.sg.SGTeleport;
import gcewing.sg.block.SGBaseBlock;
import gcewing.sg.client.SGBaseRenderer;
import gcewing.sg.entity.IrisEntity;
import gcewing.sg.menu.SGBaseMenu;
import gcewing.sg.util.AngleUtils;
import gcewing.sg.util.GateTransform;
import gcewing.sg.util.InventoryNBT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;

public class SGBaseBlockEntity extends SyncedBlockEntity implements MenuProvider {

    static final Logger LOGGER = LogUtils.getLogger();

    public static final int numRingSymbols = SGAddressing.numSymbols;
    public static final double ringSymbolAngle = 360.0 / numRingSymbols;
    public static final double irisZPosition = 0.1;
    public static final double irisThickness = 0.2;
    public static final float irisDamageAmount = 1000000;

    static final int diallingTime = 40; // ticks
    static final int interDiallingTime = 10; // ticks
    static final int transientDuration = 20; // ticks
    static final int disconnectTime = 30; // ticks

    static final double openingTransientIntensity = 1.3;
    static final double openingTransientRandomness = 0.25;
    static final double closingTransientRandomness = 0.25;
    static final double transientDamageRate = 50;

    public static final int maxIrisPhase = 60; // 3 seconds

    public static final int firstCamouflageSlot = 0;
    public static final int numCamouflageSlots = 5;
    public static final int numInventorySlots = numCamouflageSlots;

    static final float defaultChevronAngle = 40f;
    static final float[][] chevronAngles = {
        //     0    1    2    <-- Base camouflage level
        {45f, 45f, 40f}, // 7 chevrons
        {36f, 33f, 30f}  // 9 chevrons
    };

    static final Random random = new Random();

    public boolean isMerged;
    public SGState state = SGState.Idle;
    public double ringAngle, lastRingAngle, targetRingAngle; // degrees
    public int numEngagedChevrons;
    public String dialledAddress = "";
    public boolean isLinkedToController;
    public BlockPos linkedPos = BlockPos.ZERO;
    public boolean hasChevronUpgrade;
    public boolean hasIrisUpgrade;
    public IrisState irisState = IrisState.Open;
    public int irisPhase = maxIrisPhase; // 0 = fully closed, maxIrisPhase = fully open
    public int lastIrisPhase = maxIrisPhase;

    public SGLocation connectedLocation;
    public boolean isInitiator;
    int timeout;
    double energyInBuffer;
    double distanceFactor; // all energy use is multiplied by this
    boolean redstoneInput;
    boolean loaded;
    public String homeAddress;
    public String addressError = "";

    public final SimpleContainer inventory = new SimpleContainer(numInventorySlots) {
        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean stillValid(Player player) {
            return SGBaseBlockEntity.this.stillValid(player);
        }
    };

    double[][][] ehGrid;

    final List<TrackedEntity> trackedEntities = new ArrayList<>();

    public SGBaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.BASE_BE.get(), pos, state);
        inventory.addListener(c -> onInventoryChanged());
    }

    public static SGBaseBlockEntity get(net.minecraft.world.level.BlockGetter world, BlockPos pos) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof SGBaseBlockEntity base)
            return base;
        if (te instanceof SGRingBlockEntity ring)
            return ring.getBaseTE();
        return null;
    }

    public static SGBaseBlockEntity at(net.minecraft.world.level.BlockGetter world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof SGBaseBlockEntity te ? te : null;
    }

    @Override
    public String toString() {
        return String.format("SGBaseBlockEntity(%s,%s)", worldPosition, level != null ? level.dimension().location() : "?");
    }

    public Direction facing() {
        BlockState bs = getBlockState();
        return bs.hasProperty(SGBaseBlock.FACING) ? bs.getValue(SGBaseBlock.FACING) : Direction.NORTH;
    }

    public GateTransform localToGlobalTransformation() {
        return GateTransform.blockCenter(worldPosition, facing());
    }

    @Override
    public AABB getRenderBoundingBox() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new AABB(x - 2, y, z - 2, x + 3, y + 5, z + 3);
    }

    // ---------------------------------------- Lifecycle ----------------------------------------

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel && state != SGState.Idle)
            updateChunkLoadingStatus();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && !level.isClientSide)
            disconnect();
    }

    /** Called by the block when it is broken. */
    public void onRemoved() {
        if (level instanceof ServerLevel sl)
            SGChunkLoading.clear(sl, worldPosition);
    }

    void updateChunkLoadingStatus() {
        if (!(level instanceof ServerLevel sl))
            return;
        int n = SGConfig.chunkLoadingRange.get();
        if (state != SGState.Idle && n >= 0)
            SGChunkLoading.forceRange(sl, worldPosition, n);
        else
            SGChunkLoading.clear(sl, worldPosition);
    }

    public void setMerged(boolean merged) {
        if (isMerged != merged) {
            isMerged = merged;
            markChanged();
            if (SGConfig.logStargateEvents.get()) {
                String address = tryToGetHomeAddress();
                if (address != null)
                    LOGGER.info("STARGATE {} {} {} {}", isMerged ? "ADDED" : "REMOVED",
                        level.dimension().location(), worldPosition, address);
            }
            updateIrisEntity();
        }
    }

    String tryToGetHomeAddress() {
        try {
            return getHomeAddress();
        } catch (SGAddressing.AddressingError e) {
            return null;
        }
    }

    // ---------------------------------------- NBT ----------------------------------------

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        isMerged = nbt.getBoolean("isMerged");
        state = SGState.valueOf(nbt.getInt("state"));
        targetRingAngle = nbt.getDouble("targetRingAngle");
        numEngagedChevrons = nbt.getInt("numEngagedChevrons");
        dialledAddress = nbt.getString("dialledAddress");
        isLinkedToController = nbt.getBoolean("isLinkedToController");
        linkedPos = new BlockPos(nbt.getInt("linkedX"), nbt.getInt("linkedY"), nbt.getInt("linkedZ"));
        hasChevronUpgrade = nbt.getBoolean("hasChevronUpgrade");
        connectedLocation = nbt.contains("connectedLocation") ? new SGLocation(nbt.getCompound("connectedLocation")) : null;
        isInitiator = nbt.getBoolean("isInitiator");
        timeout = nbt.getInt("timeout");
        energyInBuffer = nbt.getDouble("energyInBuffer");
        distanceFactor = nbt.getDouble("distanceFactor");
        hasIrisUpgrade = nbt.getBoolean("hasIrisUpgrade");
        irisState = IrisState.valueOf(nbt.getInt("irisState"));
        irisPhase = nbt.getInt("irisPhase");
        redstoneInput = nbt.getBoolean("redstoneInput");
        homeAddress = nbt.contains("address") ? nbt.getString("address") : null;
        addressError = nbt.getString("addressError");
        InventoryNBT.load(inventory, nbt);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putBoolean("isMerged", isMerged);
        nbt.putInt("state", state.ordinal());
        nbt.putDouble("targetRingAngle", targetRingAngle);
        nbt.putInt("numEngagedChevrons", numEngagedChevrons);
        nbt.putString("dialledAddress", dialledAddress);
        nbt.putBoolean("isLinkedToController", isLinkedToController);
        nbt.putInt("linkedX", linkedPos.getX());
        nbt.putInt("linkedY", linkedPos.getY());
        nbt.putInt("linkedZ", linkedPos.getZ());
        nbt.putBoolean("hasChevronUpgrade", hasChevronUpgrade);
        if (connectedLocation != null)
            nbt.put("connectedLocation", connectedLocation.toNBT());
        nbt.putBoolean("isInitiator", isInitiator);
        nbt.putInt("timeout", timeout);
        nbt.putDouble("energyInBuffer", energyInBuffer);
        nbt.putDouble("distanceFactor", distanceFactor);
        nbt.putBoolean("hasIrisUpgrade", hasIrisUpgrade);
        nbt.putInt("irisState", irisState.ordinal());
        nbt.putInt("irisPhase", irisPhase);
        nbt.putBoolean("redstoneInput", redstoneInput);
        if (homeAddress != null)
            nbt.putString("address", homeAddress);
        if (addressError != null)
            nbt.putString("addressError", addressError);
        InventoryNBT.save(inventory, nbt);
    }

    // ---------------------------------------- State ----------------------------------------

    public boolean isActive() {
        return state != SGState.Idle && state != SGState.Disconnecting;
    }

    public boolean isConnected() {
        return state == SGState.Transient || state == SGState.Connected || state == SGState.Disconnecting;
    }

    public boolean applyChevronUpgrade(ItemStack stack, Player player) {
        if (!hasChevronUpgrade && !stack.isEmpty()) {
            if (!level.isClientSide) {
                hasChevronUpgrade = true;
                if (player == null || !player.getAbilities().instabuild)
                    stack.shrink(1);
                markChanged();
            }
            return true;
        }
        return false;
    }

    public boolean applyIrisUpgrade(ItemStack stack, Player player) {
        if (!hasIrisUpgrade && !stack.isEmpty()) {
            if (!level.isClientSide) {
                hasIrisUpgrade = true;
                if (player == null || !player.getAbilities().instabuild)
                    stack.shrink(1);
                markChanged();
                updateIrisEntity();
            }
            return true;
        }
        return false;
    }

    public int getNumChevrons() {
        return hasChevronUpgrade ? 9 : 7;
    }

    public boolean chevronIsEngaged(int i) {
        return i < numEngagedChevrons;
    }

    public float angleBetweenChevrons() {
        if (SGConfig.variableChevronPositions.get()) {
            int c9 = getNumChevrons() > 7 ? 1 : 0;
            int bc = baseCornerCamouflage();
            return chevronAngles[c9][bc];
        }
        return defaultChevronAngle;
    }

    public String getHomeAddress() throws SGAddressing.AddressingError {
        return SGAddressing.addressForLocation(new SGLocation(this));
    }

    public double interpolatedRingAngle(double t) {
        return AngleUtils.interpolateAngle(lastRingAngle, ringAngle, t);
    }

    public void tick() {
        if (level.isClientSide)
            clientUpdate();
        else {
            serverUpdate();
            checkForEntitiesInPortal();
        }
        irisUpdate();
    }

    void enterState(SGState newState, int newTimeout) {
        SGState oldState = state;
        state = newState;
        timeout = newTimeout;
        markChanged();
        if ((oldState == SGState.Idle) != (newState == SGState.Idle)) {
            updateChunkLoadingStatus();
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        if (isActive() != (oldState != SGState.Idle && oldState != SGState.Disconnecting))
            updateLinkedControllerDisplay();
        String oldDesc = sgStateDescription(oldState);
        String newDesc = sgStateDescription(newState);
        if (!oldDesc.equals(newDesc))
            postEvent("sgStargateStateChange", newDesc, oldDesc);
    }

    // ------------------------------------ DHD link -----------------------------------------

    public DHDBlockEntity getLinkedControllerTE() {
        if (isLinkedToController && level != null && level.isLoaded(linkedPos)) {
            if (level.getBlockEntity(linkedPos) instanceof DHDBlockEntity cte)
                return cte;
        }
        return null;
    }

    public void checkForLink() {
        int rangeXY = Math.max(SGConfig.linkRangeX.get(), SGConfig.linkRangeY.get());
        int rangeZ = SGConfig.linkRangeZ.get();
        for (int i = -rangeXY; i <= rangeXY; i++)
            for (int j = -rangeZ; j <= rangeZ; j++)
                for (int k = -rangeXY; k <= rangeXY; k++) {
                    BlockPos p = worldPosition.offset(i, j, k);
                    if (level.isLoaded(p) && level.getBlockEntity(p) instanceof DHDBlockEntity dhd)
                        dhd.checkForLink();
                }
    }

    public void unlinkFromController() {
        if (isLinkedToController) {
            DHDBlockEntity cte = getLinkedControllerTE();
            if (cte != null)
                cte.clearLinkToStargate();
            clearLinkToController();
        }
    }

    public void clearLinkToController() {
        isLinkedToController = false;
        markChanged();
    }

    void updateLinkedControllerDisplay() {
        DHDBlockEntity cte = getLinkedControllerTE();
        if (cte != null)
            cte.updateButtonState();
    }

    // ---------------------------------------- Server ----------------------------------------

    public void connectOrDisconnect(String address, Player player) {
        if (!address.isEmpty())
            connect(address, player);
        else
            attemptToDisconnect(player);
    }

    public String attemptToDisconnect(Player player) {
        boolean canDisconnect = disconnectionAllowed();
        SGBaseBlockEntity dte = getConnectedStargateTE();
        boolean validConnection = dte != null && !dte.isRemoved() && dte.getConnectedStargateTE() == this;
        if (canDisconnect || !validConnection) {
            if (state != SGState.Disconnecting)
                disconnect();
            return null;
        }
        return operationFailure(player, "Connection initiated from other end");
    }

    public boolean disconnectionAllowed() {
        return isInitiator || SGConfig.closeFromEitherEnd.get();
    }

    public String connect(String address, Player player) {
        SGBaseBlockEntity dte;
        if (state != SGState.Idle)
            return diallingFailure(player, "Stargate is busy");
        String homeAddress = findHomeAddress();
        if (homeAddress.isEmpty())
            return diallingFailure(player, "Coordinates of dialling stargate are out of range");
        try {
            dte = SGAddressing.findAddressedStargate(address, level);
        } catch (SGAddressing.AddressingError e) {
            return diallingFailure(player, e.getMessage());
        }
        if (dte == null || !dte.isMerged)
            return diallingFailure(player, "No stargate at address " + address);
        if (level == dte.level) {
            address = SGAddressing.localAddress(address);
            homeAddress = SGAddressing.localAddress(homeAddress);
        }
        if (address.length() > getNumChevrons())
            return diallingFailure(player, "Not enough chevrons to dial " + address);
        if (dte == this)
            return diallingFailure(player, "Stargate cannot connect to itself");
        if (dte.getNumChevrons() < homeAddress.length())
            return diallingFailure(player, "Destination stargate has insufficient chevrons");
        if (dte.state != SGState.Idle)
            return diallingFailure(player, "Stargate at address " + address + " is busy");
        distanceFactor = distanceFactorForCoordDifference(this, dte);
        if (!energyIsAvailable(SGConfig.energyToOpen() * distanceFactor))
            return diallingFailure(player, "Stargate has insufficient energy");
        startDiallingStargate(address, dte, true);
        dte.startDiallingStargate(homeAddress, this, false);
        return null;
    }

    public static double distanceFactorForCoordDifference(BlockEntity te1, BlockEntity te2) {
        double dx = te1.getBlockPos().getX() - te2.getBlockPos().getX();
        double dz = te1.getBlockPos().getZ() - te2.getBlockPos().getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        double ld = Math.log(0.05 * d + 1);
        double lm = Math.log(0.05 * 16 * SGAddressing.coordRange);
        double lr = ld / lm;
        double f = 1 + 14 * SGConfig.distanceFactorMultiplier.get() * lr * lr;
        if (te1.getLevel() != te2.getLevel())
            f *= SGConfig.interDimensionMultiplier.get();
        return f;
    }

    public void playSGSoundEffect(SoundEvent se, float volume, float pitch) {
        level.playSound(null, worldPosition, se, SoundSource.BLOCKS,
            volume * SGConfig.soundVolume.get().floatValue(), pitch);
    }

    String diallingFailure(Player player, String mess) {
        if (player != null && state == SGState.Idle)
            playSGSoundEffect(ModRegistry.SOUND_ABORT.get(), 1.0F, 1.0F);
        return operationFailure(player, mess);
    }

    String operationFailure(Player player, String mess) {
        if (player != null)
            sendChatMessage(player, Component.literal(mess));
        return mess;
    }

    public static void sendChatMessage(Player player, MutableComponent mess) {
        player.displayClientMessage(mess, false);
    }

    String findHomeAddress() {
        try {
            return getHomeAddress();
        } catch (SGAddressing.AddressingError e) {
            return "";
        }
    }

    public void disconnect() {
        SGBaseBlockEntity dte = connectedLocation != null ? connectedLocation.getStargateTE() : null;
        if (dte != null && dte != this)
            dte.clearConnection();
        clearConnection();
    }

    public void clearConnection() {
        if (state != SGState.Idle || connectedLocation != null) {
            dialledAddress = "";
            connectedLocation = null;
            isInitiator = false;
            numEngagedChevrons = 0;
            markChanged();
            if (state == SGState.Connected) {
                enterState(SGState.Disconnecting, disconnectTime);
                playSGSoundEffect(ModRegistry.SOUND_CLOSE.get(), 1.0F, 1.0F);
            } else {
                if (state != SGState.Idle && state != SGState.Disconnecting)
                    playSGSoundEffect(ModRegistry.SOUND_ABORT.get(), 1.0F, 1.0F);
                enterState(SGState.Idle, 0);
            }
        }
    }

    void startDiallingStargate(String address, SGBaseBlockEntity dte, boolean initiator) {
        dialledAddress = address;
        connectedLocation = new SGLocation(dte);
        isInitiator = initiator;
        setChanged();
        startDiallingNextSymbol();
        postEvent(initiator ? "sgDialOut" : "sgDialIn", address);
    }

    void serverUpdate() {
        if (!loaded) {
            loaded = true;
            try {
                homeAddress = getHomeAddress();
                addressError = "";
            } catch (SGAddressing.AddressingError e) {
                homeAddress = null;
                addressError = e.getMessage();
            }
            // Gates generated by world generation (or placed without a block update) merge here.
            if (!isMerged)
                ((SGBaseBlock) getBlockState().getBlock()).checkForMerge(level, worldPosition);
            else if (!isLinkedToController)
                checkForLink();
            updateIrisEntity();
            markChanged();
        }
        if (isMerged) {
            tickEnergyUsage();
            if (timeout > 0) {
                if (state == SGState.Transient && !irisIsClosed())
                    performTransientDamage();
                --timeout;
            } else
                switch (state) {
                    case Idle -> {
                        if (undialledDigitsRemaining())
                            startDiallingNextSymbol();
                    }
                    case Dialling -> finishDiallingSymbol();
                    case InterDialling -> startDiallingNextSymbol();
                    case Transient -> enterState(SGState.Connected, isInitiator ? SGConfig.ticksToStayOpen() : 0);
                    case Connected -> {
                        if (isInitiator && SGConfig.ticksToStayOpen() > 0)
                            disconnect();
                    }
                    case Disconnecting -> enterState(SGState.Idle, 0);
                }
        }
    }

    void tickEnergyUsage() {
        if (state == SGState.Connected && isInitiator)
            if (!useEnergy(SGConfig.energyUsePerTick() * distanceFactor))
                disconnect();
    }

    public double availableEnergy() {
        List<ISGEnergySource> sources = findEnergySources();
        return energyInBuffer + energyAvailableFrom(sources);
    }

    boolean energyIsAvailable(double amount) {
        return availableEnergy() >= amount;
    }

    boolean useEnergy(double amount) {
        if (amount <= energyInBuffer) {
            energyInBuffer -= amount;
            setChanged();
            return true;
        }
        List<ISGEnergySource> sources = findEnergySources();
        double energyAvailable = energyInBuffer + energyAvailableFrom(sources);
        if (amount > energyAvailable)
            return false;
        double desiredEnergy = Math.max(amount, SGConfig.maxEnergyBuffer.get());
        double targetEnergy = Math.min(desiredEnergy, energyAvailable);
        double energyRequired = targetEnergy - energyInBuffer;
        double energyOnHand = energyInBuffer + drawEnergyFrom(sources, energyRequired);
        if (amount - 0.0001 > energyOnHand) {
            LOGGER.warn("Energy sources only delivered {} of promised {}", energyOnHand - energyInBuffer, energyAvailable);
            return false;
        }
        setEnergyInBuffer(energyOnHand - amount);
        return true;
    }

    List<ISGEnergySource> findEnergySources() {
        List<ISGEnergySource> result = new ArrayList<>();
        GateTransform t = localToGlobalTransformation();
        for (int i = -2; i <= 2; i++) {
            BlockPos bp = t.blockPos(i, -1, 0);
            if (level.getBlockEntity(bp) instanceof ISGEnergySource src)
                result.add(src);
        }
        DHDBlockEntity te = getLinkedControllerTE();
        if (te != null)
            result.add(te);
        return result;
    }

    double energyAvailableFrom(List<ISGEnergySource> sources) {
        double energy = 0;
        for (ISGEnergySource source : sources)
            energy += source.availableEnergy();
        return energy;
    }

    double drawEnergyFrom(List<ISGEnergySource> sources, double amount) {
        double total = 0;
        for (ISGEnergySource source : sources) {
            if (total >= amount)
                break;
            total += source.drawEnergy(amount - total);
        }
        return total;
    }

    void setEnergyInBuffer(double amount) {
        if (energyInBuffer != amount) {
            energyInBuffer = amount;
            setChanged();
        }
    }

    void performTransientDamage() {
        GateTransform t = localToGlobalTransformation();
        AABB box = t.box(new Vec3(-1.5, 0.5, 0.5), new Vec3(1.5, 3.5, 5.5));
        Vec3 gp = t.p(0, 2, 0.5);
        for (LivingEntity ent : level.getEntitiesOfClass(LivingEntity.class, box)) {
            double dist = Math.min(ent.position().distanceTo(gp), 1.0);
            int damage = (int) Math.ceil(dist * transientDamageRate);
            ent.hurt(SGDamage.transientDamage(level), damage);
        }
    }

    boolean undialledDigitsRemaining() {
        return numEngagedChevrons < dialledAddress.length();
    }

    void startDiallingNextSymbol() {
        startDiallingSymbol(dialledAddress.charAt(numEngagedChevrons));
    }

    void startDiallingSymbol(char c) {
        int i = SGAddressing.charToSymbol(c);
        if (i >= 0 && i < numRingSymbols) {
            startDiallingToAngle(i * ringSymbolAngle);
            playSGSoundEffect(ModRegistry.SOUND_DIAL.get(), 1.0F, 1.0F);
        } else {
            LOGGER.warn("Stargate jammed trying to dial symbol {}", c);
            dialledAddress = "";
            enterState(SGState.Idle, 0);
        }
    }

    void startDiallingToAngle(double a) {
        targetRingAngle = AngleUtils.normaliseAngle(a);
        enterState(SGState.Dialling, diallingTime);
    }

    void finishDiallingSymbol() {
        ++numEngagedChevrons;
        String symbol = dialledAddress.substring(numEngagedChevrons - 1, numEngagedChevrons);
        postEvent("sgChevronEngaged", numEngagedChevrons, symbol);
        if (undialledDigitsRemaining())
            enterState(SGState.InterDialling, interDiallingTime);
        else
            finishDiallingAddress();
    }

    void finishDiallingAddress() {
        if (!isInitiator || useEnergy(SGConfig.energyToOpen() * distanceFactor)) {
            enterState(SGState.Transient, transientDuration);
            playSGSoundEffect(ModRegistry.SOUND_OPEN.get(), 1.0F, 1.0F);
        } else
            disconnect();
    }

    boolean canTravelFromThisEnd() {
        return isInitiator || !SGConfig.oneWayTravel.get();
    }

    record TrackedEntity(Entity entity, Vec3 lastPos) {
    }

    void checkForEntitiesInPortal() {
        if (state == SGState.Connected) {
            List<TrackedEntity> tracked = new ArrayList<>(trackedEntities);
            trackedEntities.clear();
            for (TrackedEntity trk : tracked)
                entityInPortal(trk.entity, trk.lastPos);
            if (state != SGState.Connected)
                return;
            GateTransform t = localToGlobalTransformation();
            AABB box = t.box(new Vec3(-1.5, 0.5, -3.5), new Vec3(1.5, 3.5, 3.5));
            for (Entity entity : level.getEntities((Entity) null, box, e -> true)) {
                if (entity instanceof FishingHook || entity instanceof IrisEntity)
                    continue;
                if (entity.isAlive() && entity.getVehicle() == null)
                    trackedEntities.add(new TrackedEntity(entity, entity.position()));
            }
        } else
            trackedEntities.clear();
    }

    void entityInPortal(Entity entity, Vec3 prevPos) {
        if (entity.isAlive() && entity.level() == level && state == SGState.Connected && canTravelFromThisEnd()) {
            GateTransform t = localToGlobalTransformation();
            Vec3 pos = entity.position();
            Vec3 p1 = t.ip(pos);
            Vec3 p0 = t.ip(prevPos.scale(2).subtract(pos));
            double z0 = 0.0;
            if (p0.z >= z0 && p1.z < z0 && p1.z > z0 - 5.0) {
                Vec3 v = pos.subtract(prevPos);
                SGBaseBlockEntity dte = getConnectedStargateTE();
                if (dte != null) {
                    while (entity.getVehicle() != null)
                        entity = entity.getVehicle();
                    SGTeleport.teleportEntityAndRiders(entity, v, t, dte.localToGlobalTransformation(),
                        (ServerLevel) dte.level, dte.irisIsClosed());
                }
            }
        }
    }

    public SGBaseBlockEntity getConnectedStargateTE() {
        if (isConnected() && connectedLocation != null)
            return connectedLocation.getStargateTE();
        return null;
    }

    // ---------------------------------------- Client ----------------------------------------

    @Override
    protected void onClientUpdate() {
        // The ring blocks render the camouflage stored in this block entity.
        refreshRingModels();
    }

    SGState lastClientState = SGState.Idle;

    void clientUpdate() {
        if (isMerged && state != lastClientState) {
            switch (state) {
                case Transient -> initiateOpeningTransient();
                case Disconnecting -> initiateClosingTransient();
                default -> {
                }
            }
        }
        lastClientState = state;
        lastRingAngle = ringAngle;
        switch (state) {
            case Dialling -> updateRingAngle();
            case Transient, Connected, Disconnecting -> {
                applyRandomImpulse();
                updateEventHorizon();
            }
            default -> {
            }
        }
    }

    void updateRingAngle() {
        if (timeout > 0) {
            double da = AngleUtils.diffAngle(ringAngle, targetRingAngle) / timeout;
            ringAngle = AngleUtils.addAngle(ringAngle, da);
            --timeout;
        } else
            ringAngle = targetRingAngle;
    }

    public double[][][] getEventHorizonGrid() {
        if (ehGrid == null) {
            int m = SGBaseRenderer.ehGridRadialSize;
            int n = SGBaseRenderer.ehGridPolarSize;
            ehGrid = new double[2][n + 2][m + 1];
            for (int i = 0; i < 2; i++) {
                ehGrid[i][0] = ehGrid[i][n];
                ehGrid[i][n + 1] = ehGrid[i][1];
            }
        }
        return ehGrid;
    }

    void initiateOpeningTransient() {
        double[][] v = getEventHorizonGrid()[1];
        int n = SGBaseRenderer.ehGridPolarSize;
        for (int j = 0; j <= n + 1; j++) {
            v[j][0] = openingTransientIntensity;
            v[j][1] = v[j][0] + openingTransientRandomness * random.nextGaussian();
        }
    }

    void initiateClosingTransient() {
        double[][] v = getEventHorizonGrid()[1];
        int m = SGBaseRenderer.ehGridRadialSize;
        int n = SGBaseRenderer.ehGridPolarSize;
        for (int i = 1; i < m; i++)
            for (int j = 1; j <= n; j++)
                v[j][i] += closingTransientRandomness * random.nextGaussian();
    }

    void applyRandomImpulse() {
        double[][] v = getEventHorizonGrid()[1];
        int m = SGBaseRenderer.ehGridRadialSize;
        int n = SGBaseRenderer.ehGridPolarSize;
        int i = random.nextInt(m - 1) + 1;
        int j = random.nextInt(n) + 1;
        v[j][i] += 0.05 * random.nextGaussian();
    }

    void updateEventHorizon() {
        double[][][] grid = getEventHorizonGrid();
        double[][] u = grid[0];
        double[][] v = grid[1];
        int m = SGBaseRenderer.ehGridRadialSize;
        int n = SGBaseRenderer.ehGridPolarSize;
        double dt = 1.0;
        double asq = 0.03;
        double d = 0.95;
        for (int i = 1; i < m; i++)
            for (int j = 1; j <= n; j++) {
                double du_dr = 0.5 * (u[j][i + 1] - u[j][i - 1]);
                double d2u_drsq = u[j][i + 1] - 2 * u[j][i] + u[j][i - 1];
                double d2u_dthsq = u[j + 1][i] - 2 * u[j][i] + u[j - 1][i];
                v[j][i] = d * v[j][i] + (asq * dt) * (d2u_drsq + du_dr / i + d2u_dthsq / (i * i));
            }
        for (int i = 1; i < m; i++)
            for (int j = 1; j <= n; j++)
                u[j][i] += v[j][i] * dt;
        double u0 = 0, v0 = 0;
        for (int j = 1; j <= n; j++) {
            u0 += u[j][1];
            v0 += v[j][1];
        }
        u0 /= n;
        v0 /= n;
        for (int j = 1; j <= n; j++) {
            u[j][0] = u0;
            v[j][0] = v0;
        }
    }

    // ----------------------------------------- Iris -----------------------------------------

    public boolean irisIsClosed() {
        return hasIrisUpgrade && irisPhase <= maxIrisPhase / 2;
    }

    public double getIrisAperture(double t) {
        return (lastIrisPhase * (1 - t) + irisPhase * t) / maxIrisPhase;
    }

    void irisUpdate() {
        lastIrisPhase = irisPhase;
        switch (irisState) {
            case Opening -> {
                if (irisPhase < maxIrisPhase)
                    ++irisPhase;
                else if (!level.isClientSide)
                    enterIrisState(IrisState.Open);
            }
            case Closing -> {
                if (irisPhase > 0)
                    --irisPhase;
                else if (!level.isClientSide)
                    enterIrisState(IrisState.Closed);
            }
            default -> {
            }
        }
    }

    void enterIrisState(IrisState newState) {
        if (irisState != newState) {
            String oldDesc = irisStateDescription(irisState);
            String newDesc = irisStateDescription(newState);
            irisState = newState;
            markChanged();
            if (!level.isClientSide) {
                switch (newState) {
                    case Opening -> playSGSoundEffect(ModRegistry.SOUND_IRIS_OPEN.get(), 1.0F, 1.0F);
                    case Closing -> playSGSoundEffect(ModRegistry.SOUND_IRIS_CLOSE.get(), 1.0F, 1.0F);
                    default -> {
                    }
                }
            }
            if (!oldDesc.equals(newDesc))
                postEvent("sgIrisStateChange", newDesc, oldDesc);
        }
    }

    public void openIris() {
        if (isMerged && hasIrisUpgrade && irisState != IrisState.Open)
            enterIrisState(IrisState.Opening);
    }

    public void closeIris() {
        if (isMerged && hasIrisUpgrade && irisState != IrisState.Closed)
            enterIrisState(IrisState.Closing);
    }

    public void onNeighborBlockChange() {
        if (!level.isClientSide) {
            boolean newInput = isExternallyPowered();
            if (redstoneInput != newInput) {
                redstoneInput = newInput;
                setChanged();
                if (redstoneInput)
                    closeIris();
                else
                    openIris();
            }
        }
    }

    /**
     * Redstone input from anything other than this block's own output (port of Greg's
     * blockIsGettingExternallyPowered). While a gate is active it emits a signal itself, and
     * vanilla's hasNeighborSignal would see that signal reflected back through adjacent blocks.
     */
    boolean isExternallyPowered() {
        for (Direction dir : Direction.values()) {
            BlockPos npos = worldPosition.relative(dir);
            BlockState ns = level.getBlockState(npos);
            if (ns.is(getBlockState().getBlock()))
                continue;
            if (ns.getSignal(level, npos, dir) > 0)
                return true;
            if (ns.isRedstoneConductor(level, npos))
                for (Direction dir2 : Direction.values())
                    if (dir2 != dir.getOpposite() && level.getDirectSignal(npos.relative(dir2), dir2) > 0)
                        return true;
        }
        return false;
    }

    void updateIrisEntity() {
        if (level == null || level.isClientSide)
            return;
        if (isMerged && hasIrisUpgrade) {
            if (findIrisEntities().isEmpty())
                level.addFreshEntity(new IrisEntity(this));
        } else {
            for (IrisEntity ent : findIrisEntities())
                ent.discard();
        }
    }

    List<IrisEntity> findIrisEntities() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        AABB box = new AABB(x - 2, y, z - 2, x + 3, y + 5, z + 3);
        return level.getEntitiesOfClass(IrisEntity.class, box, e -> worldPosition.equals(e.basePos));
    }

    // -------------------------------------- Camouflage --------------------------------------

    /** Camouflage block state for a gate block in the bottom row, or null. */
    public BlockState getCamouflageState(BlockPos cpos) {
        GateTransform t = localToGlobalTransformation();
        Vec3 p = t.ip(Vec3.atCenterOf(cpos));
        if (Math.abs(p.y) < 0.01) {
            int i = 2 + (int) Math.round(p.x);
            if (i >= 0 && i < 5)
                return camouflageStateFor(inventory.getItem(firstCamouflageSlot + i));
        }
        return null;
    }

    public static BlockState camouflageStateFor(ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi) {
            Block block = bi.getBlock();
            return block.defaultBlockState();
        }
        return null;
    }

    public static boolean isValidCamouflage(ItemStack stack) {
        return stack.getItem() instanceof BlockItem bi && !(bi.getBlock() instanceof net.minecraft.world.level.block.EntityBlock);
    }

    void onInventoryChanged() {
        if (level != null && !level.isClientSide && !InventoryNBT.loading)
            markChanged();
    }

    void refreshRingModels() {
        if (level == null)
            return;
        requestModelDataUpdate();
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos p = worldPosition.offset(dx, 0, dz);
                BlockEntity be = level.getBlockEntity(p);
                if (be != null) {
                    be.requestModelDataUpdate();
                    level.sendBlockUpdated(p, be.getBlockState(), be.getBlockState(), 8);
                }
            }
    }

    @Override
    public ModelData getModelData() {
        return CamouflageData.of(isMerged, isMerged ? getCamouflageState(worldPosition) : null);
    }

    protected int baseCornerCamouflage() {
        return Math.max(baseCamouflageAt(0), baseCamouflageAt(4));
    }

    protected int baseCamouflageAt(int i) {
        BlockState cs = camouflageStateFor(inventory.getItem(i));
        if (cs != null) {
            if (cs.getBlock() instanceof SlabBlock)
                return 1;
            if (level != null && cs.isCollisionShapeFullBlock(level, worldPosition))
                return 2;
        }
        return 0;
    }

    // --------------------------------------- Menu ---------------------------------------

    public boolean stillValid(Player player) {
        return !isRemoved() && player.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= 64.0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.sgcraft.stargate_address");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SGBaseMenu(id, playerInv, this);
    }

    // ------------------------------------ Computer interface ----------------------------------

    /** Positions of blocks that can attach to the stargate (the row below the ring). */
    public List<BlockPos> adjacentTilePositions() {
        List<BlockPos> result = new ArrayList<>();
        GateTransform t = localToGlobalTransformation();
        for (int i = -2; i <= 2; i++)
            result.add(t.blockPos(i, -1, 0));
        return result;
    }

    public String sendMessage(Object[] args) {
        SGBaseBlockEntity dte = getConnectedStargateTE();
        if (dte != null) {
            dte.postEvent("sgMessageReceived", args);
            return null;
        }
        return "Stargate not connected";
    }

    void postEvent(String name, Object... args) {
        if (level == null || level.isClientSide)
            return;
        for (BlockPos p : adjacentTilePositions()) {
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof IComputerInterface ci)
                ci.postEvent(this, name, args);
        }
    }

    public String sgStateDescription() {
        return sgStateDescription(state);
    }

    static String sgStateDescription(SGState state) {
        return switch (state) {
            case Idle -> "Idle";
            case Dialling, InterDialling -> "Dialling";
            case Transient -> "Opening";
            case Connected -> "Connected";
            case Disconnecting -> "Closing";
        };
    }

    public String irisStateDescription() {
        return irisStateDescription(irisState);
    }

    static String irisStateDescription(IrisState state) {
        return state.toString();
    }

    public double getDistanceFactor() {
        return distanceFactor;
    }
}
