package com.giacconidev.balancer.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giacconidev.balancer.backend.dto.BotDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import reactor.core.publisher.Flux;

import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Profile("!test") 
public class BotCommanderSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(BotCommanderSocketHandler.class);

    private final BotService botService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public BotCommanderSocketHandler(BotService botService) {
        this.botService = botService;
    }

    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        logger.info("New WebSocket connection established: {}", session.getId());
        Flux<BotDto> activeBots = botService.getAllActiveBots();

        activeBots.doOnNext(data -> {
            try {
                // initial message with list of orders
                String json = objectMapper.writeValueAsString(data);
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).subscribe();

        activeBots.doOnComplete(() -> {
            sessions.add(session);
        }).subscribe();
    }

    @Override
    public void handleMessage(@NonNull WebSocketSession session, @NonNull WebSocketMessage<?> message)
            throws Exception {

    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {

    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        if (session.isOpen())
            session.close();
        logger.info("WebSocket connection closed: {}", session.getId());
        sessions.remove(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    public void broadcastUpdate(Object object) {
        sessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    logger.info("Sending message to session: {}", session.getId());
                    String json = objectMapper.writeValueAsString(object);
                    session.sendMessage(new TextMessage(json));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}