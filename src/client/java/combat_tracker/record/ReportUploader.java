package combat_tracker.record;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class ReportUploader {
    private static final Logger LOGGER = LoggerFactory.getLogger("combat_tracker/upload");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static final String ENDPOINT = "https://aegisac.netlify.app/api/report";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private ReportUploader() {
    }

    public static void report(SessionData d, String server, String html, String canonical, boolean blocking) {
        String endpoint = ENDPOINT;
        if (endpoint == null || endpoint.isBlank() || endpoint.contains("YOUR-SITE")) {
            return;
        }

        String body;
        HttpRequest req;
        try {
            body = GSON.toJson(buildEnvelope(d, server, html, canonical));
            req = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(blocking ? 3 : 10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
        } catch (Exception e) {
            LOGGER.debug("Could not build report request", e);
            return;
        }

        if (blocking) {
            try {
                CLIENT.send(req, HttpResponse.BodyHandlers.discarding());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
            }
        } else {
            CLIENT.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(t -> null);
        }
    }

    private static Envelope buildEnvelope(SessionData d, String server, String html, String canonical) {
        Envelope e = new Envelope();
        e.sessionId = d.playerUuid + "-" + d.startEpochMs;
        e.player = d.player;
        e.uuid = d.playerUuid;
        e.accountType = d.accountType;
        e.server = (server == null || server.isBlank()) ? "singleplayer" : server;
        e.startUtc = d.startUtc;
        e.endUtc = d.endUtc;
        e.detected = detectedList(d);
        e.flags = new Flags(d.flagHotbar, d.flagUse, d.flagAttack, d.flagKeybind);
        e.sources = d.flagSources == null ? List.of() : d.flagSources;
        e.html = html;
        e.canonical = canonical;
        return e;
    }

    private static List<String> detectedList(SessionData d) {
        List<String> out = new ArrayList<>();
        if (d.flagAttack > 0) out.add("Synthetic Attack");
        if (d.flagKeybind > 0) out.add("Synthetic Keybind Press");
        if (d.flagHotbar > 0) out.add("Synthetic Hotbar Switch");
        if (d.flagUse > 0) out.add("Synthetic Use Item");
        return out;
    }

    private static final class Envelope {
        String sessionId;
        String player;
        String uuid;
        String accountType;
        String server;
        String startUtc;
        String endUtc;
        List<String> detected;
        Flags flags;
        List<SessionData.SourceEntry> sources;
        String html;
        String canonical;
    }

    private record Flags(int hotbar, int use, int attack, int keybind) {
    }
}
