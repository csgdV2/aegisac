package combat_tracker.detection;

import combat_tracker.CombatTrackerClient;
import combat_tracker.record.SessionRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class ComboTracker {
    private static final ComboTracker INSTANCE = new ComboTracker();

    public static ComboTracker get() {
        return INSTANCE;
    }

    private static final long COMBO_GAP_NANO = 700L * 1_000_000L;

    private int currentTargetId = -1;
    private long lastHitNano = 0L;
    private int comboHits = 0;
    private int prevHurtTime = 0;

    public void onAttack(Entity target) {
        LocalPlayer self = Minecraft.getInstance().player;
        if (self == null || target == self || !(target instanceof Player)) {
            return;
        }
        if (!self.isSprinting()) {
            return;
        }

        long clickNano = CombatTrackerClient.clickNano;
        long nano = clickNano != 0L ? clickNano : System.nanoTime();

        if (nano <= lastHitNano) {
            nano = System.nanoTime();
        }
        long maxGapNano = COMBO_GAP_NANO;

        if (target.getId() == currentTargetId && comboHits >= 1 && (nano - lastHitNano) <= maxGapNano) {
            long intervalMs = (nano - lastHitNano) / 1_000_000L;
            comboHits++;
            boolean newCombo = comboHits == 2;
            SessionRecorder.get().recordCombo(intervalMs, newCombo);
        } else {
            currentTargetId = target.getId();
            comboHits = 1;
        }
        lastHitNano = nano;
    }

    public void tick(LocalPlayer player) {
        if (prevHurtTime == 0 && player.hurtTime > 0) {
            breakCombo();
        }
        prevHurtTime = player.hurtTime;

        if (currentTargetId != -1) {
            long maxGapNano = COMBO_GAP_NANO;
            if (System.nanoTime() - lastHitNano > maxGapNano) {
                breakCombo();
            }
        }
    }

    public int currentCombo() {
        return comboHits;
    }

    private void breakCombo() {
        currentTargetId = -1;
        comboHits = 0;
    }
}
