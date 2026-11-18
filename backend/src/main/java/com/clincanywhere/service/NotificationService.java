package com.clincanywhere.service;

import com.clincanywhere.dto.NotificationResponse;
import com.clincanywhere.entity.UserNotification;
import com.clincanywhere.repository.UserNotificationRepository;
import com.clincanywhere.security.RecordScope;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_REPLAY = 100;
    private static final int RETENTION_DAYS = 30;

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate websocket;
    private final RecordScope scope;
    private final UserNotificationRepository notifications;
    private final ObjectProvider<NotificationEventBroadcaster> eventBroadcaster;

    public NotificationService(SimpMessagingTemplate websocket, RecordScope scope,
                               UserNotificationRepository notifications,
                               ObjectProvider<NotificationEventBroadcaster> eventBroadcaster) {
        this.websocket = websocket;
        this.scope = scope;
        this.notifications = notifications;
        this.eventBroadcaster = eventBroadcaster;
    }

    public SseEmitter subscribe(String clinic, String username, String lastEventId) {
        scope.requirePeerMember(clinic, username);
        String key = clinic + ":" + username;
        SseEmitter emitter = new SseEmitter(30L * 60 * 1000);
        CopyOnWriteArrayList<SseEmitter> subscribers = emitters.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>());
        subscribers.add(emitter);
        Runnable remove = () -> removeEmitter(key, subscribers, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());

        try {
            emitter.send(SseEmitter.event().comment("connected").reconnectTime(5000));
            Long afterId = parseEventId(lastEventId);
            if (afterId != null) {
                List<UserNotification> batch;
                do {
                    batch = notifications.findByClinicIdAndRecipientAndIdGreaterThanOrderByIdAsc(
                            clinic, username, afterId, PageRequest.of(0, MAX_REPLAY));
                    for (UserNotification item : batch) {
                        send(key, subscribers, emitter, response(item));
                        afterId = item.getId();
                    }
                } while (batch.size() == MAX_REPLAY);
            }
        } catch (IOException | IllegalStateException failure) {
            remove.run();
        }
        return emitter;
    }

    @Async("applicationTaskExecutor")
    public void publish(String clinic, String recipient, String message) {
        scope.requirePeerMember(clinic, recipient);
        UserNotification saved = notifications.save(new UserNotification(clinic, recipient, message));
        NotificationResponse event = response(saved);
        NotificationEventBroadcaster broadcaster = eventBroadcaster.getIfAvailable();
        if (broadcaster == null) deliverToLocalSubscribers(event);
        else {
            try { broadcaster.publish(event); }
            catch (RuntimeException unavailable) {
                log.warn("Shared notification fanout failed; delivering locally exceptionType={}", unavailable.getClass().getSimpleName());
                deliverToLocalSubscribers(event);
            }
        }
        try {
            websocket.convertAndSendToUser(java.util.Objects.requireNonNull(recipient), "/queue/notifications", java.util.Objects.requireNonNull(event));
        } catch (RuntimeException realtimeUnavailable) {
            log.warn("Realtime notification delivery failed exceptionType={}", realtimeUnavailable.getClass().getSimpleName());
        }
    }

    public void deliverToLocalSubscribers(NotificationResponse event) {
        String key = event.clinicId() + ":" + event.recipient();
        var subscribers = emitters.get(key);
        if (subscribers != null) subscribers.forEach(emitter -> send(key, subscribers, emitter, event));
    }

    public List<NotificationResponse> recent(String clinic, String username, int limit) {
        scope.requirePeerMember(clinic, username);
        return notifications.findByClinicIdAndRecipientOrderByIdDesc(clinic, username, PageRequest.of(0, limit))
                .stream().map(this::response).toList();
    }

    @Scheduled(fixedDelayString = "${SSE_HEARTBEAT_MS:15000}")
    public void heartbeat() {
        emitters.forEach((key, subscribers) -> subscribers.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().comment("keepalive"));
            } catch (IOException | IllegalStateException disconnected) {
                removeEmitter(key, subscribers, emitter);
            }
        }));
    }

    @Scheduled(cron = "${NOTIFICATION_RETENTION_CRON:0 30 2 * * *}")
    @Transactional
    public void purgeExpired() {
        notifications.deleteByCreatedAtBefore(Instant.now().minus(Duration.ofDays(RETENTION_DAYS)));
    }

    private void send(String key, CopyOnWriteArrayList<SseEmitter> subscribers,
                      SseEmitter emitter, NotificationResponse event) {
        try {
            emitter.send(SseEmitter.event().name("notification").id(java.util.Objects.requireNonNull(event.id())).data(event));
        } catch (IOException | IllegalStateException disconnected) {
            removeEmitter(key, subscribers, emitter);
        }
    }

    private void removeEmitter(String key, CopyOnWriteArrayList<SseEmitter> subscribers, SseEmitter emitter) {
        subscribers.remove(emitter);
        if (subscribers.isEmpty()) emitters.remove(key, subscribers);
    }

    private @Nullable Long parseEventId(String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) return null;
        try { return Math.max(0L, Long.parseLong(lastEventId)); }
        catch (NumberFormatException invalidId) { return 0L; }
    }

    private NotificationResponse response(UserNotification notification) {
        Long id = notification.getId();
        if (id == null) throw new IllegalStateException("Persisted notification has no identifier");
        return new NotificationResponse(id.toString(), notification.getMessage(),
                notification.getCreatedAt(), notification.getClinicId(), notification.getRecipient());
    }
}
