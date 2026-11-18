package com.clincanywhere.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.clincanywhere.dto.MessageRequest;
import com.clincanywhere.entity.DirectMessage;
import com.clincanywhere.repository.DirectMessageRepository;
import com.clincanywhere.security.RecordScope;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@SuppressWarnings("null") // Mockito-generated doubles and matchers are passed to non-null APIs.
class MessagingServiceTest {
    @Test
    void persistsMessageBeforePublishingRealtimeAndNotificationEvents() {
        DirectMessageRepository repository = mock(DirectMessageRepository.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        RecordScope scope = mock(RecordScope.class);
        DirectMessage unsaved = new DirectMessage("clinic-a", "doctor", "reception", "Please confirm");
        when(repository.saveAndFlush(any(DirectMessage.class))).thenReturn(unsaved);
        MessagingService service = new MessagingService(repository, events, scope);

        var response = service.send("clinic-a", "doctor", new MessageRequest("reception", " Please confirm "));

        assertEquals("doctor", response.sender());
        assertEquals("reception", response.recipient());
        assertEquals("Please confirm", response.body());
        InOrder order = inOrder(repository, events);
        order.verify(repository).saveAndFlush(any(DirectMessage.class));
        order.verify(events).publishEvent(new DirectMessageCreated("clinic-a", "doctor", "reception", response));
    }
}
