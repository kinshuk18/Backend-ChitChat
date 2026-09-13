package com.example.chitchat.websocket;

import com.example.chitchat.dto.ChatMessageResponse;
import com.example.chitchat.service.MessageService;
import jakarta.annotation.PreDestroy;
import org.postgresql.PGConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

/**
 * Background listener that keeps one dedicated PostgreSQL connection per
 * instance, LISTENs on the fan-out channel, and re-dispatches every event to
 * the sockets connected to THIS instance.
 *
 * Together with WsEventPublisher this gives every instance the illusion of a
 * single shared session registry: an event raised on any node is delivered by
 * the node actually holding each recipient's socket. Delivery path is single
 * (always via NOTIFY, including for the originating instance), so no
 * de-duplication is required.
 */
@Component
public class WsEventListener {

    private final MessageService messageService;
    private final WebsocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @Value("${spring.datasource.url}")
    private String url;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;

    private volatile boolean running = true;
    private Thread worker;

    public WsEventListener(MessageService messageService,
                           WebsocketSessionManager sessionManager,
                           ObjectMapper objectMapper) {
        this.messageService = messageService;
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        worker = new Thread(this::runLoop, "ws-event-listener");
        worker.setDaemon(true);
        worker.start();
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (worker != null) worker.interrupt();
    }

    private void runLoop() {
        while (running) {
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement st = conn.createStatement()) {

                st.execute("LISTEN " + WsEventPublisher.CHANNEL);
                System.out.println("[WsListener] LISTENing on channel '" +
                        WsEventPublisher.CHANNEL + "'");

                PGConnection pg = conn.unwrap(PGConnection.class);
                while (running) {
                    var notifications = pg.getNotifications();
                    if (notifications == null || notifications.length == 0) {
                        Thread.sleep(300);   // pgjdbc delivers async notices on poll
                        continue;
                    }
                    for (var n : notifications) {
                        handle(n.getParameter());
                    }
                }
            } catch (InterruptedException e) {
                return; // shutdown requested
            } catch (Exception e) {
                if (!running) return;
                System.out.println("[WsListener] channel connection lost, retrying in 3s: "
                        + e.getMessage());
                try { Thread.sleep(3000); } catch (InterruptedException ie) { return; }
            }
        }
    }

    private void handle(String payload) {
        try {
            WsEventPublisher.Envelope env =
                    objectMapper.readValue(payload, WsEventPublisher.Envelope.class);

            switch (env.t()) {
                case "MSG" -> {
                    // Hydrate from the shared DB (verify signature + decrypt) and push
                    // to sockets connected locally on this instance.
                    ChatMessageResponse message =
                            messageService.getLiveMessage(UUID.fromString(env.m()));
                    if (message != null) {
                        sessionManager.broadcastLocally(UUID.fromString(env.r()), message);
                    }
                }
                case "STA" -> sessionManager.broadcastStatusLocally(
                        UUID.fromString(env.m()), UUID.fromString(env.r()), env.u());
                case "SYS" -> sessionManager.broadcastRawLocally(
                        UUID.fromString(env.r()), env.p());
                default -> System.out.println("[WsListener] unknown event type: " + env.t());
            }
        } catch (Exception e) {
            System.out.println("[WsListener] could not process event '" + payload + "': "
                    + e.getMessage());
        }
    }
}
