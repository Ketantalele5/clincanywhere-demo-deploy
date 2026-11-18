package com.clincanywhere.controller;

import com.clincanywhere.dto.NotificationResponse;
import com.clincanywhere.service.NotificationService;
import java.security.Principal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.jspecify.annotations.Nullable;
import java.util.List;

@RestController
@RequestMapping("/api/{clinicId}/notifications")
public class NotificationController {
    private final NotificationService notifications;
    public NotificationController(NotificationService notifications) { this.notifications = notifications; }
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String clinicId,
                             @RequestHeader(value = "Last-Event-ID", required = false) @Nullable String lastEventId,
                             Principal principal) {
        return notifications.subscribe(clinicId, principal.getName(), lastEventId);
    }
    @GetMapping
    public List<NotificationResponse> recent(@PathVariable String clinicId, @RequestParam(defaultValue = "50") int limit, Principal principal) {
        return notifications.recent(clinicId, principal.getName(), Math.max(1, Math.min(limit, 100)));
    }
}
