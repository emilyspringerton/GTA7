package industrial.einhorn.gta7;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Real IDUNA integration: authenticates as the GTA7-SERVER agent (registered
// 2026-08-05, migration 202608050002_gta7_server_agent.sql) and talks HTTP
// directly, same as every Go service in this monorepo -- no more shelling
// out to the emily CLI (that was VS0's shortcut; this replaces it).
//
// Also registers a real WOTAN-shared player_id per Minecraft player
// (provider=minecraft, provider_sub=Bukkit UUID) via IDUNA's existing
// /api/v1/players/register -- the same generic player-identity registry
// REDGARDEN-BOTS already uses, so a GTA7 player and a WOTAN/SHANKPIT player
// are the same underlying IDUNA identity if they're the same person.
// Flow/Field-Office numbers themselves stay in GTA7's own YAML for now --
// WOTAN's kills/deaths/sessions schema is SHANKPIT-shaped and shouldn't be
// repurposed to mean something else.
final class IdunaClient {

    private static final String AGENT_NAME = "GTA7-SERVER";
    private static final Pattern TOKEN_RE = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern EXPIRES_RE = Pattern.compile("\"expires_in\"\\s*:\\s*(\\d+)");
    private static final Pattern PLAYER_ID_RE = Pattern.compile("\"player_id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CHAT_OBJECT_RE = Pattern.compile("\\{[^{}]*\\}");
    private static final Pattern ID_RE = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
    private static final Pattern SENDER_NAME_RE = Pattern.compile("\"sender_name\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern SENDER_SOURCE_RE = Pattern.compile("\"sender_source\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern BODY_RE = Pattern.compile("\"body\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    // Minimal shape for a chat_messages row (see IDUNA's ChatMessagesHandler).
    // No player_id/UUID -- this endpoint's sender identity is display-name-
    // only by design, same as its existing mud/battlegrounds bridge.
    record ChatMessage(long id, String senderName, String senderSource, String body) {}

    private final JavaPlugin plugin;
    private final String baseUrl;
    private final String agentSecret;
    private final HttpClient http = HttpClient.newHttpClient();

    private volatile String token;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    IdunaClient(JavaPlugin plugin, String baseUrl, Path secretsFile) {
        this.plugin = plugin;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.agentSecret = readSecret(secretsFile);
    }

    boolean isConfigured() {
        return agentSecret != null && !agentSecret.isBlank();
    }

    private static String readSecret(Path secretsFile) {
        try {
            for (String line : Files.readAllLines(secretsFile)) {
                String trimmed = line.trim().replaceFirst("^export\\s+", "");
                if (trimmed.startsWith("IDUNA_SECRET_GTA7_SERVER=")) {
                    return trimmed.substring("IDUNA_SECRET_GTA7_SERVER=".length()).trim();
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    private synchronized String ensureToken() {
        if (token != null && Instant.now().isBefore(tokenExpiry.minusSeconds(300))) {
            return token;
        }
        String body = "{\"agent_name\":\"" + AGENT_NAME + "\",\"agent_secret\":\"" + agentSecret + "\"}";
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/auth/agent"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new IOException("iduna auth " + resp.statusCode() + ": " + resp.body());
            }
            Matcher tm = TOKEN_RE.matcher(resp.body());
            if (!tm.find()) throw new IOException("no access_token in response: " + resp.body());
            token = tm.group(1);

            Matcher em = EXPIRES_RE.matcher(resp.body());
            long expiresIn = em.find() ? Long.parseLong(em.group(1)) : 900;
            tokenExpiry = Instant.now().plusSeconds(expiresIn);
            return token;
        } catch (IOException | InterruptedException e) {
            throw new UncheckedIOException("IDUNA auth failed", e instanceof IOException ioe ? ioe : new IOException(e));
        }
    }

    // Fire-and-log: a failed Apple post shouldn't break gameplay, just gets logged.
    void postApple(String appleType, String title, String body, String runId) {
        try {
            String jsonBody = "{"
                    + "\"apple_type\":\"" + escape(appleType) + "\","
                    + "\"title\":\"" + escape(title) + "\","
                    + "\"body\":\"" + escape(body) + "\","
                    + "\"source_repo\":\"GTA7\","
                    + "\"run_id\":\"" + escape(runId) + "\""
                    + "}";
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/apples"))
                    .header("Authorization", "Bearer " + ensureToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                plugin.getLogger().warning("IDUNA apple post failed (" + resp.statusCode() + "): " + resp.body());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("IDUNA apple post failed: " + e.getMessage());
        }
    }

    // Registers/looks up a real IDUNA player_id for a Minecraft player. Returns
    // null on failure (never throws) -- identity linking is a nice-to-have,
    // not something that should be able to break claim/contest gameplay.
    String registerPlayer(String minecraftUuid, String displayName) {
        try {
            String jsonBody = "{"
                    + "\"provider\":\"minecraft\","
                    + "\"provider_sub\":\"" + escape(minecraftUuid) + "\","
                    + "\"display_name\":\"" + escape(displayName) + "\""
                    + "}";
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/players/register"))
                    .header("Authorization", "Bearer " + ensureToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                plugin.getLogger().warning("IDUNA player register failed (" + resp.statusCode() + "): " + resp.body());
                return null;
            }
            Matcher m = PLAYER_ID_RE.matcher(resp.body());
            return m.find() ? m.group(1) : null;
        } catch (Exception e) {
            plugin.getLogger().warning("IDUNA player register failed: " + e.getMessage());
            return null;
        }
    }

    // S171-04 chat bridge: posts to the real, already-existing
    // /api/v1/chat/messages endpoint (built for mud<->battlegrounds,
    // extended for gfd_server/einhorn_survival -- see
    // GoblinFoxDragon/docs2/CHAT_BRIDGE_TO_EINHORN_SURVIVAL_SPEC.md).
    // Fire-and-log, same as postApple -- a failed chat relay shouldn't
    // break local chat.
    void postChat(String senderName, String body) {
        try {
            String jsonBody = "{"
                    + "\"channel\":\"gta7\","
                    + "\"sender_name\":\"" + escape(senderName) + "\","
                    + "\"sender_source\":\"einhorn_survival\","
                    + "\"body\":\"" + escape(body) + "\""
                    + "}";
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/chat/messages"))
                    .header("Authorization", "Bearer " + ensureToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                plugin.getLogger().warning("IDUNA chat post failed (" + resp.statusCode() + "): " + resp.body());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("IDUNA chat post failed: " + e.getMessage());
        }
    }

    // Polls for new messages since sinceId, GFD-origin only (filtered
    // client-side -- the real endpoint has no exclude_server param, see the
    // spec doc's own note on this). Never throws; returns empty on failure
    // so a transient IDUNA hiccup doesn't spam warnings every poll tick.
    List<ChatMessage> pollGfdChat(long sinceId) {
        List<ChatMessage> out = new ArrayList<>();
        try {
            HttpRequest req = HttpRequest.newBuilder(
                            URI.create(baseUrl + "/api/v1/chat/messages?since_id=" + sinceId + "&limit=50"))
                    .header("Authorization", "Bearer " + ensureToken())
                    .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                plugin.getLogger().warning("IDUNA chat poll failed (" + resp.statusCode() + "): " + resp.body());
                return out;
            }
            Matcher objMatcher = CHAT_OBJECT_RE.matcher(resp.body());
            while (objMatcher.find()) {
                String obj = objMatcher.group();
                Matcher sourceM = SENDER_SOURCE_RE.matcher(obj);
                if (!sourceM.find() || !"gfd_server".equals(unescape(sourceM.group(1)))) continue;

                Matcher idM = ID_RE.matcher(obj);
                Matcher nameM = SENDER_NAME_RE.matcher(obj);
                Matcher bodyM = BODY_RE.matcher(obj);
                if (idM.find() && nameM.find() && bodyM.find()) {
                    out.add(new ChatMessage(Long.parseLong(idM.group(1)),
                            unescape(nameM.group(1)), "gfd_server", unescape(bodyM.group(1))));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("IDUNA chat poll failed: " + e.getMessage());
        }
        return out;
    }

    private static String unescape(String s) {
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
