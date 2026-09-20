package combat_tracker.detection;

import combat_tracker.CombatTrackerClient;
import combat_tracker.config.CtConfig;
import combat_tracker.config.TimingWindow;
import combat_tracker.record.SessionRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class JumpResetTracker {
    private static final int JUMP_LOOKBACK_TICKS = 2;
    private static final int RESULT_COOLDOWN_TICKS = 4;

    private static final long MAX_ATTEMPT_MS = 200;

    private static final double KNOCKBACK_THRESHOLD = 0.065;

    private static final int WINDOW_TICKS_GROUND = 6;

    private static final int WINDOW_TICKS_AIR = 10;

    private enum State { IDLE, WINDOW_ACTIVE }

    private State state = State.IDLE;

    private int currentTick = 0;
    private int lastResultTick = Integer.MIN_VALUE / 2;
    private int lastJumpTick = Integer.MIN_VALUE / 2;
    private long lastJumpNano = 0L;

    private int hitTick = 0;
    private long hitNano = 0L;
    private boolean hitWasGrounded = true;

    private final LatencyEstimator latency = LatencyEstimator.get();

    private int prevHurtTime = 0;
    private boolean prevOnGround = true;

    public void tick(Minecraft client) {
        currentTick++;

        LocalPlayer player = client.player;
        if (player == null) {
            reset();
            return;
        }

        long tickNano = System.nanoTime();

        double vx = player.getDeltaMovement().x;
        double vz = player.getDeltaMovement().z;
        double horizMag = Math.sqrt(vx * vx + vz * vz);
        boolean onGround = player.onGround();
        int hurtTime = player.hurtTime;
        int invulnTime = player.invulnerableTime;
        latency.sample(client);

        long jumpSignalNano = CombatTrackerClient.consumeJumpNano();
        boolean jumpNow = jumpSignalNano != 0L;
        if (jumpNow) {
            lastJumpTick = currentTick;
            lastJumpNano = jumpSignalNano;
        }

        boolean hitNow = prevHurtTime == 0
                && hurtTime > 0
                && invulnTime > 0
                && horizMag > KNOCKBACK_THRESHOLD;
        if (hitNow) {
            handleHit(tickNano);
        }

        if (state == State.WINDOW_ACTIVE) {
            int maxTicks = hitWasGrounded ? WINDOW_TICKS_GROUND : WINDOW_TICKS_AIR;
            if (currentTick - hitTick > maxTicks) {
                state = State.IDLE;
            }
        }

        if (jumpNow && state == State.WINDOW_ACTIVE && readyForResult()) {
            double ms = (lastJumpNano - hitNano) / 1_000_000.0;
            registerAttempt(ms);
            state = State.IDLE;
        }

        prevHurtTime = hurtTime;
        prevOnGround = onGround;
    }

    private void handleHit(long tickNano) {
        long hitAtNano = CombatTrackerClient.hitNano != 0L ? CombatTrackerClient.hitNano : tickNano;

        int ticksSinceJump = currentTick - lastJumpTick;

        if (ticksSinceJump >= 0 && ticksSinceJump <= JUMP_LOOKBACK_TICKS && readyForResult()) {
            registerAttempt((lastJumpNano - hitAtNano) / 1_000_000.0);
            state = State.IDLE;
            return;
        }

        if (state == State.WINDOW_ACTIVE) {
            hitTick = currentTick;
            hitNano = hitAtNano;
            hitWasGrounded = prevOnGround;
        } else if (state == State.IDLE && readyForResult()) {
            hitTick = currentTick;
            hitNano = hitAtNano;
            hitWasGrounded = prevOnGround;
            state = State.WINDOW_ACTIVE;
        }
    }

    private boolean readyForResult() {
        return currentTick - lastResultTick >= RESULT_COOLDOWN_TICKS;
    }

    private void registerAttempt(double ms) {
        long delta = Math.round(ms);
        if (Math.abs(delta) > MAX_ATTEMPT_MS) {
            return;
        }
        lastResultTick = currentTick;

        TimingWindow window = CtConfig.get().window;
        TimingWindow.Result result = window.classify(delta);

        SessionRecorder.get().recordJump(delta, result.name());
    }

    private void reset() {
        state = State.IDLE;
        prevHurtTime = 0;
        prevOnGround = true;
        lastJumpTick = Integer.MIN_VALUE / 2;
        lastJumpNano = 0L;
        latency.reset();
    }
}
