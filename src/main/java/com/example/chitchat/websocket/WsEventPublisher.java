package com.example.chitchat.websocket;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * Publishes WebSocket fan-out events onto a PostgreSQL NOTIFY channel so that
 * EVERY application instance receives them, regardless of which instance a
 * given user socket is connected to.
 *
 * Why: WebsocketSessionManager keeps sockets in JVM-local memory. With N
 * instances behind a load balancer, a broadcast must reach rooms whose members
 * are spread across all instances. PostgreSQL doubles as the message bus since
 * we already share one database - no extra infrastructure needed.
 *
 * Event types:
 *   MSG - a message was persisted; receivers hydrate it from the shared DB.
 *   STA - a delivery/read receipt changed; receivers rebuild state from the DB.
 *   SYS - ephemeral system events (join/leave notices); travel inline because
 *         they are never persisted. Must stay under the 8000-byte NOTIFY cap.
 */
@Component
public class WsEventPublisher {

    public static final String CHANNEL = "chitchat_ws";
    private static final int MAX_NOTIFY_BYTES = 7500;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public WsEventPublisher(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Wire format carried inside pg_notify payloads.
     * t = event type (MSG | STA | SYS)
     * r = room id, m = message id, u = acting username, p = raw inline JSON (SYS only)
     */
    public record Envelope(String t, String r, String m, String u, String p) {}

    public void publishMessage(UUID roomId, UUID messageId) {
        publish(new Envelope("MSG", roomId.toString(), messageId.toString(), null, null));
    }

    public void publishStatus(UUID roomId, UUID messageId, String username) {
        publish(new Envelope("STA", roomId.toString(), messageId.toString(), username, null));
    }

    public void publishRaw(UUID roomId, Object event) {
        try {
            String inline = objectMapper.writeValueAsString(event);
            if (inline.length() > MAX_NOTIFY_BYTES) {
                System.out.println("[WsPublisher] event too large for NOTIFY (" +
                        inline.length() + " bytes), dropped");
                return;
            }
            publish(new Envelope("SYS", roomId.toString(), null, null, inline));
        } catch (Exception e) {
            System.out.println("[WsPublisher] failed to serialize system event: " + e.getMessage());
        }
    }

    private void publish(Envelope envelope) {
        try {
            String payload = objectMapper.writeValueAsString(envelope);
            // pg_notify delivers to every LISTENer on the channel, on every instance.
            jdbcTemplate.queryForObject("SELECT pg_notify(?, ?)", String.class,
                    WsEventPublisher.CHANNEL, payload);
        } catch (Exception e) {
            System.out.println("[WsPublisher] failed to publish event: " + e.getMessage());
        }
    }
}
