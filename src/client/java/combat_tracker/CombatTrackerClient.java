package combat_tracker;

import combat_tracker.detection.ClickTimestamps;
import combat_tracker.detection.ComboTracker;
import combat_tracker.detection.InputContext;
import combat_tracker.detection.IntegrityMonitor;
import combat_tracker.detection.JumpResetTracker;
import combat_tracker.detection.LatencyEstimator;
import combat_tracker.net.AegisHandshake;
import combat_tracker.record.SessionRecorder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CombatTrackerClient implements ClientModInitializer {
    public static final String MOD_ID = "combat_tracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static volatile long jumpNano = 0L;

    public static volatile long hitNano = 0L;

    public static volatile long clickNano = 0L;

    public static long consumeJumpNano() {
        long v = jumpNano;
        jumpNano = 0L;
        return v;
    }

    private final JumpResetTracker tracker = new JumpResetTracker();

    private static final String MOD_VERSION = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");

    @Override
    public void onInitializeClient() {
        AegisHandshake.register();

        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            SessionRecorder.get().startAuto();
            AegisHandshake.requestSend();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SessionRecorder.get().onDisconnect();
            AegisHandshake.reset();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> SessionRecorder.get().onClientStopping());

        LOGGER.info("AegisAC initialized (client-side, observational only)");
    }

    private void onEndTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            ClickTimestamps.clear();
        }
        if (player != null && client.level != null) {
            AegisHandshake.tick(MOD_VERSION);
            tracker.tick(client);
            ComboTracker.get().tick(player);
            IntegrityMonitor.get().tick(player);
            SessionRecorder.get().samplePing(LatencyEstimator.get().currentMs());
            SessionRecorder.get().noteIdentity(player.getName().getString(), player.getUUID().toString());
            SessionRecorder.get().noteHeldItem(player.getMainHandItem());
            SessionRecorder.get().captureServer();
        }

        InputContext.resetForTick();
    }
}
