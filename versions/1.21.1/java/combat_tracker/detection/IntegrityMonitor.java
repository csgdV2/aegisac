package combat_tracker.detection;

import net.minecraft.client.player.LocalPlayer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Pre-1.21.5 variant: identical to the main IntegrityMonitor except that hotbar-slot
// drift detection is disabled. Inventory.getSelectedSlot() does not exist before
// 1.21.5, so the slot-observing path is a no-op here. Synthetic keybind / use /
// attack detection and source attribution (which feed the uploaded report) work
// exactly as on the default build.
public final class IntegrityMonitor {
    private static final int MAX_SOURCES = 24;

    public enum Kind {
        HOTBAR,
        USE,
        ATTACK,
        KEYBIND
    }

    private static final IntegrityMonitor INSTANCE = new IntegrityMonitor();

    public static IntegrityMonitor get() {
        return INSTANCE;
    }

    private final SlotLedger slots = new SlotLedger();

    private WeakReference<LocalPlayer> lastPlayer;

    private volatile int hotbarFlags;
    private volatile int useFlags;
    private volatile int attackFlags;
    private volatile int keybindFlags;

    private final Map<String, Tally> sources = new ConcurrentHashMap<>();

    private static final class Tally {
        final String kind;
        final FlagOrigin origin;
        final AtomicInteger count = new AtomicInteger();

        volatile List<String> suspects;

        Tally(String kind, FlagOrigin origin) {
            this.kind = kind;
            this.origin = origin;
            this.suspects = origin.suspects();
        }
    }

    public record SourceCount(String key, String kind, FlagOrigin origin, int count) {
    }

    private IntegrityMonitor() {
    }

    public int hotbarFlags() {
        return hotbarFlags;
    }

    public int useFlags() {
        return useFlags;
    }

    public int attackFlags() {
        return attackFlags;
    }

    public int keybindFlags() {
        return keybindFlags;
    }

    public int totalFlags() {
        return hotbarFlags + useFlags + attackFlags + keybindFlags;
    }

    public List<SourceCount> sourceCounts() {
        List<SourceCount> out = new ArrayList<>(sources.size());
        for (Map.Entry<String, Tally> e : sources.entrySet()) {
            Tally t = e.getValue();
            out.add(new SourceCount(e.getKey(), t.kind, t.origin.withSuspects(t.suspects),
                    t.count.get()));
        }
        return out;
    }

    public void reset() {
        hotbarFlags = 0;
        useFlags = 0;
        attackFlags = 0;
        keybindFlags = 0;
        sources.clear();
        slots.clear();
        lastPlayer = null;
    }

    public void onSyntheticKeybind() {
        keybindFlags++;
        record(Kind.KEYBIND, SourceAttribution.attribute());
    }

    public void noteSetterCall(int newSlot, InputContext.Source source) {
        // No hotbar-slot ledger on this version (see class note).
    }

    public void onSilentSlotPacket() {
        hotbarFlags++;
        record(Kind.HOTBAR, SourceAttribution.attribute());
    }

    public void onUseItem() {
        if (!InputContext.inKeybinds()) {
            useFlags++;
            record(Kind.USE, SourceAttribution.attribute());
        }
    }

    public void onAttack() {
        if (!InputContext.inKeybinds()) {
            attackFlags++;
            record(Kind.ATTACK, SourceAttribution.attribute());
        }
    }

    public void tick(LocalPlayer player) {
        // Hotbar-slot drift detection unavailable before 1.21.5 (see class note).
    }

    public void checkSlotNow() {
        // Hotbar-slot drift detection unavailable before 1.21.5 (see class note).
    }

    private void record(Kind kind, FlagOrigin origin) {
        String name = kind.name().toLowerCase(Locale.ROOT);
        String key = name + "|" + origin.key();
        Tally t = sources.get(key);
        if (t == null) {
            if (sources.size() >= MAX_SOURCES) {
                return;
            }
            t = sources.computeIfAbsent(key, k -> new Tally(name, origin));
        }
        t.count.incrementAndGet();
        if (!origin.suspects().isEmpty()) {
            t.suspects = origin.suspects();
        }
    }
}
