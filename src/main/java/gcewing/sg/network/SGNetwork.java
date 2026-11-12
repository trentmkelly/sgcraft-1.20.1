package gcewing.sg.network;

import java.util.function.Supplier;

import gcewing.sg.SGAddressing;
import gcewing.sg.SGCraft;
import gcewing.sg.block.entity.DHDBlockEntity;
import gcewing.sg.block.entity.SGBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Client-to-server messages sent by the DHD dialling screen (replaces SGChannel). */
public final class SGNetwork {

    static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(SGCraft.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private SGNetwork() {
    }

    public static void init() {
        CHANNEL.messageBuilder(DHDAddressMessage.class, 0, NetworkDirection.PLAY_TO_SERVER)
            .encoder(DHDAddressMessage::encode).decoder(DHDAddressMessage::decode)
            .consumerMainThread(DHDAddressMessage::handle).add();
    }

    /** What the DHD screen wants done with the address being entered. */
    public enum Action { SET_ENTERED_ADDRESS, CONNECT_OR_DISCONNECT }

    public record DHDAddressMessage(BlockPos dhdPos, Action action, String address) {

        static void encode(DHDAddressMessage msg, FriendlyByteBuf buf) {
            buf.writeBlockPos(msg.dhdPos);
            buf.writeEnum(msg.action);
            buf.writeUtf(msg.address, 32);
        }

        static DHDAddressMessage decode(FriendlyByteBuf buf) {
            return new DHDAddressMessage(buf.readBlockPos(), buf.readEnum(Action.class), buf.readUtf(32));
        }

        static void handle(DHDAddressMessage msg, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !player.level().isLoaded(msg.dhdPos)
                || player.distanceToSqr(Vec3.atCenterOf(msg.dhdPos)) > 64.0)
                return;
            if (!(player.level().getBlockEntity(msg.dhdPos) instanceof DHDBlockEntity dhd))
                return;
            String address = msg.address;
            if (address.length() > SGAddressing.maxAddressLength)
                return;
            for (int i = 0; i < address.length(); i++)
                if (!SGAddressing.isValidSymbolChar(address.charAt(i)))
                    return;
            switch (msg.action) {
                case SET_ENTERED_ADDRESS -> dhd.setEnteredAddress(address);
                case CONNECT_OR_DISCONNECT -> {
                    SGBaseBlockEntity gate = dhd.getLinkedStargateTE();
                    if (gate != null)
                        gate.connectOrDisconnect(address, player);
                }
            }
        }
    }
}
