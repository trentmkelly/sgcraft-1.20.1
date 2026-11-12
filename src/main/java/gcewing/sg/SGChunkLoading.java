package gcewing.sg;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.world.ForgeChunkManager;

/** Keeps the chunks around active stargates loaded (replaces Greg's BaseTEChunkManager). */
public final class SGChunkLoading {

    private SGChunkLoading() {
    }

    public static void init() {
        // Connections are closed when the server stops, so any tickets left from a previous run are stale.
        // An active gate re-forces its chunks when it loads.
        ForgeChunkManager.setForcedChunkLoadingCallback(SGCraft.MODID, (level, helper) -> {
            for (BlockPos owner : helper.getBlockTickets().keySet())
                helper.removeAllTickets(owner);
        });
    }

    public static void forceRange(ServerLevel level, BlockPos owner, int range) {
        ChunkPos c = new ChunkPos(owner);
        for (int i = -range; i <= range; i++)
            for (int j = -range; j <= range; j++)
                ForgeChunkManager.forceChunk(level, SGCraft.MODID, owner, c.x + i, c.z + j, true, true);
    }

    public static void clear(ServerLevel level, BlockPos owner) {
        ChunkPos c = new ChunkPos(owner);
        int range = Math.max(SGConfig.chunkLoadingRange.get(), 0);
        for (int i = -range; i <= range; i++)
            for (int j = -range; j <= range; j++)
                ForgeChunkManager.forceChunk(level, SGCraft.MODID, owner, c.x + i, c.z + j, false, true);
    }
}
