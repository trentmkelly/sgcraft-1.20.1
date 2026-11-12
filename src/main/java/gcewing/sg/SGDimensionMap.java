package gcewing.sg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Persistent mapping between dimensions and the small integers encoded in stargate addresses.
 * Dimensions are numbered in order of first use, so addresses stay stable for the life of a world.
 */
public class SGDimensionMap extends SavedData {

    static final String NAME = "sgcraft_dimension_map";

    protected final List<ResourceKey<Level>> indexToDimension = new ArrayList<>();
    protected final Map<ResourceKey<Level>, Integer> dimensionToIndex = new HashMap<>();

    public static SGDimensionMap get() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server.overworld().getDataStorage().computeIfAbsent(SGDimensionMap::load, SGDimensionMap::create, NAME);
    }

    static SGDimensionMap create() {
        SGDimensionMap map = new SGDimensionMap();
        // Give the vanilla dimensions the first indices so their addresses are the same in every world.
        map.add(Level.OVERWORLD);
        map.add(Level.NETHER);
        map.add(Level.END);
        map.setDirty();
        return map;
    }

    static SGDimensionMap load(CompoundTag nbt) {
        SGDimensionMap map = new SGDimensionMap();
        ListTag list = nbt.getList("dimensions", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++)
            map.add(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(list.getString(i))));
        return map;
    }

    void add(ResourceKey<Level> dimension) {
        dimensionToIndex.put(dimension, indexToDimension.size());
        indexToDimension.add(dimension);
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (ResourceKey<Level> key : indexToDimension)
            list.add(StringTag.valueOf(key.location().toString()));
        nbt.put("dimensions", list);
        return nbt;
    }

    public static ResourceKey<Level> dimensionForIndex(int index) {
        SGDimensionMap map = get();
        if (index >= 0 && index < map.indexToDimension.size())
            return map.indexToDimension.get(index);
        return null;
    }

    public static int indexForDimension(ResourceKey<Level> dimension) {
        SGDimensionMap map = get();
        Integer index = map.dimensionToIndex.get(dimension);
        if (index == null) {
            index = map.indexToDimension.size();
            map.add(dimension);
            map.setDirty();
        }
        return index;
    }
}
