package gcewing.sg;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration options. Names and defaults follow the original SGCraft.cfg categories.
 */
public class SGConfig {

    public static final ForgeConfigSpec SPEC;

    // stargate
    public static final ForgeConfigSpec.DoubleValue energyPerFuelItem;
    public static final ForgeConfigSpec.IntValue gateOpeningsPerFuelItem;
    public static final ForgeConfigSpec.IntValue minutesOpenPerFuelItem;
    public static final ForgeConfigSpec.IntValue secondsToStayOpen;
    public static final ForgeConfigSpec.BooleanValue oneWayTravel;
    public static final ForgeConfigSpec.BooleanValue closeFromEitherEnd;
    public static final ForgeConfigSpec.DoubleValue maxEnergyBuffer;
    public static final ForgeConfigSpec.DoubleValue distanceFactorMultiplier;
    public static final ForgeConfigSpec.DoubleValue interDimensionMultiplier;
    public static final ForgeConfigSpec.BooleanValue transparency;
    public static final ForgeConfigSpec.DoubleValue soundVolume;
    public static final ForgeConfigSpec.BooleanValue variableChevronPositions;
    public static final ForgeConfigSpec.IntValue explosionRadius;
    public static final ForgeConfigSpec.BooleanValue explosionFlame;
    public static final ForgeConfigSpec.BooleanValue explosionSmoke;

    // iris
    public static final ForgeConfigSpec.BooleanValue preserveInventory;

    // dhd
    public static final ForgeConfigSpec.IntValue linkRangeX;
    public static final ForgeConfigSpec.IntValue linkRangeY;
    public static final ForgeConfigSpec.IntValue linkRangeZ;

    // options
    public static final ForgeConfigSpec.IntValue chunkLoadingRange;
    public static final ForgeConfigSpec.BooleanValue logStargateEvents;
    public static final ForgeConfigSpec.BooleanValue augmentStructures;
    public static final ForgeConfigSpec.IntValue structureAugmentationChance;

    // naquadah
    public static final ForgeConfigSpec.BooleanValue enableNaquadahOre;
    public static final ForgeConfigSpec.IntValue genUnderLavaOdds;
    public static final ForgeConfigSpec.IntValue maxNodesUnderLava;
    public static final ForgeConfigSpec.IntValue genIsolatedOdds;
    public static final ForgeConfigSpec.IntValue maxIsolatedNodes;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("stargate");
        energyPerFuelItem = b.comment("Stargate energy units supplied by one piece of naquadah")
            .defineInRange("energyPerFuelItem", 96000.0, 1.0, Double.MAX_VALUE);
        gateOpeningsPerFuelItem = b.comment("Number of gate openings one piece of naquadah pays for (at distance factor 1)")
            .defineInRange("gateOpeningsPerFuelItem", 24, 1, Integer.MAX_VALUE);
        minutesOpenPerFuelItem = b.comment("Minutes a gate can be held open by one piece of naquadah (at distance factor 1)")
            .defineInRange("minutesOpenPerFuelItem", 80, 1, Integer.MAX_VALUE);
        secondsToStayOpen = b.comment("Seconds before an outgoing connection closes automatically (0 = never)")
            .defineInRange("secondsToStayOpen", 300, 0, Integer.MAX_VALUE);
        oneWayTravel = b.comment("Only allow travel from the dialling gate to the dialled gate")
            .define("oneWayTravel", false);
        closeFromEitherEnd = b.comment("Allow a connection to be closed from the receiving end")
            .define("closeFromEitherEnd", true);
        maxEnergyBuffer = b.comment("Maximum energy a stargate draws from its sources in advance")
            .defineInRange("maxEnergyBuffer", 1000.0, 0.0, Double.MAX_VALUE);
        distanceFactorMultiplier = b.defineInRange("distanceFactorMultiplier", 1.0, 0.0, Double.MAX_VALUE);
        interDimensionMultiplier = b.defineInRange("interDimensionMultiplier", 4.0, 0.0, Double.MAX_VALUE);
        transparency = b.comment("Render the event horizon translucent (client)")
            .define("transparency", true);
        soundVolume = b.defineInRange("soundVolume", 1.0, 0.0, 10.0);
        variableChevronPositions = b.comment("Move the chevrons closer together when the base is camouflaged")
            .define("variableChevronPositions", true);
        explosionRadius = b.comment("Size of the explosion when a connected gate is broken (0 = no explosion)")
            .defineInRange("explosionRadius", 10, 0, 100);
        explosionFlame = b.define("explosionFlame", true);
        explosionSmoke = b.define("explosionSmoke", true);
        b.pop();

        b.push("iris");
        preserveInventory = b.comment("Don't destroy a player's inventory when they hit a closed iris")
            .define("preserveInventory", false);
        b.pop();

        b.push("dhd");
        linkRangeX = b.comment("Blocks either side of the DHD searched for a stargate")
            .defineInRange("linkRangeX", 5, 0, 16);
        linkRangeY = b.comment("Blocks up or down from the DHD searched for a stargate")
            .defineInRange("linkRangeY", 1, 0, 16);
        linkRangeZ = b.comment("Blocks in front of the DHD searched for a stargate")
            .defineInRange("linkRangeZ", 6, 1, 16);
        b.pop();

        b.push("options");
        chunkLoadingRange = b.comment("Radius in chunks kept loaded around an active stargate (-1 = no chunk loading)")
            .defineInRange("chunkLoadingRange", 1, -1, 8);
        logStargateEvents = b.define("logStargateEvents", false);
        augmentStructures = b.comment("Generate buried stargates under some desert pyramids")
            .define("augmentStructures", false);
        structureAugmentationChance = b.comment("Percent chance of a desert pyramid having a stargate under it")
            .defineInRange("structureAugmentationChance", 25, 0, 100);
        b.pop();

        b.push("naquadah");
        enableNaquadahOre = b.define("enableNaquadahOre", true);
        genUnderLavaOdds = b.comment("1 in N chunks get naquadah nodes under lava")
            .defineInRange("genUnderLavaOdds", 1, 1, 1000);
        maxNodesUnderLava = b.defineInRange("maxNodesUnderLava", 16, 1, 256);
        genIsolatedOdds = b.comment("1 in N chunks get isolated naquadah nodes")
            .defineInRange("genIsolatedOdds", 1, 1, 1000);
        maxIsolatedNodes = b.defineInRange("maxIsolatedNodes", 4, 1, 256);
        b.pop();

        SPEC = b.build();
    }

    // Derived values (see original SGBaseTE.configure)

    public static double energyToOpen() {
        return energyPerFuelItem.get() / gateOpeningsPerFuelItem.get();
    }

    public static double energyUsePerTick() {
        return energyPerFuelItem.get() / (minutesOpenPerFuelItem.get() * 60.0 * 20.0);
    }

    public static int ticksToStayOpen() {
        return 20 * secondsToStayOpen.get();
    }
}
