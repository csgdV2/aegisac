package combat_tracker.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Lets a server prove the client is running AegisAC. 26.2 variant: the Fabric networking
 * API (v6) renamed the registry accessor {@code playC2S()} to {@code serverboundPlay()};
 * this overlay exists solely to use the new name. Behaviour is identical to the main-tree
 * copy.
 *
 * <p>The mod registers a client→server custom-payload channel ({@code aegisac:handshake})
 * and, once connected, sends a single message carrying the mod version. A server-side
 * plugin (see {@code plugin-src/ForceAegisAC}) that registers the same channel receives
 * that message and knows the player has the mod. This is the only network traffic the mod
 * sends to the game server; the actual reports still go to the dashboard, not the server.
 */
public final class AegisHandshake {
    /** Shared channel id — must match the server plugin's channel exactly. */
    public static final Identifier CHANNEL =
            Identifier.fromNamespaceAndPath("aegisac", "handshake");

    private static volatile boolean pending = false;
    private static volatile boolean sent = false;

    private AegisHandshake() {
    }

    /** The payload: just the mod version so the server can log/verify it. */
    public record Payload(String version) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(CHANNEL);
        public static final StreamCodec<FriendlyByteBuf, Payload> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUtf(p.version()),
                buf -> new Payload(buf.readUtf()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Register the outgoing channel. Call once during client init. */
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(Payload.TYPE, Payload.CODEC);
    }

    /** Queue a handshake to be sent as soon as the server accepts the channel. */
    public static void requestSend() {
        pending = true;
        sent = false;
    }

    /** Clear state on disconnect so the next join sends a fresh handshake. */
    public static void reset() {
        pending = false;
        sent = false;
    }

    /**
     * Send the handshake once the connection is ready. Safe to call every client tick:
     * it does nothing until a send is pending and the server has declared the channel,
     * then fires exactly once.
     */
    public static void tick(String version) {
        if (!pending || sent) {
            return;
        }
        if (ClientPlayNetworking.canSend(Payload.TYPE)) {
            ClientPlayNetworking.send(new Payload(version));
            sent = true;
            pending = false;
        }
    }
}
