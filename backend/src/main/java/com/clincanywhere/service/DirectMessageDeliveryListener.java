package com.clincanywhere.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DirectMessageDeliveryListener {
    private static final Logger log = LoggerFactory.getLogger(DirectMessageDeliveryListener.class);
    private final SimpMessagingTemplate websocket;
    private final NotificationService notifications;

    public DirectMessageDeliveryListener(SimpMessagingTemplate websocket, NotificationService notifications) {
        this.websocket = websocket;
        this.notifications = notifications;
    }

    @Async("applicationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void deliver(DirectMessageCreated event) {
        try {
            websocket.convertAndSendToUser(java.util.Objects.requireNonNull(event.recipient()), "/queue/messages", java.util.Objects.requireNonNull(event.message()));
            websocket.convertAndSendToUser(java.util.Objects.requireNonNull(event.sender()), "/queue/messages", java.util.Objects.requireNonNull(event.message()));
        } catch (RuntimeException unavailable) {
            log.warn("Message persisted but WebSocket delivery failed exceptionType={}", unavailable.getClass().getSimpleName());
        }
        notifications.publish(event.clinicId(), event.recipient(), "New message from " + event.sender());
    }
}
