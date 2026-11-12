package gcewing.sg.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Client-only entry points called from common code via DistExecutor. */
public final class ClientHooks {

    private ClientHooks() {
    }

    public static void openDHDScreen(BlockPos pos) {
        Minecraft.getInstance().setScreen(new DHDScreen(pos));
    }
}
