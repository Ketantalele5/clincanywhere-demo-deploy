package com.clincanywhere.service;

import com.clincanywhere.security.RecordScope;

import com.clincanywhere.common.exception.BadRequestException;
import com.clincanywhere.dto.MessageRequest;
import com.clincanywhere.dto.MessageResponse;
import com.clincanywhere.entity.DirectMessage;
import com.clincanywhere.repository.DirectMessageRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessagingService {
    private final DirectMessageRepository messages;
    private final ApplicationEventPublisher events;
    private final RecordScope scope;
    private final FieldEncryptionService encryption;

    @org.springframework.beans.factory.annotation.Autowired
    public MessagingService(DirectMessageRepository messages,
                            ApplicationEventPublisher events, RecordScope scope, FieldEncryptionService encryption) {
        this.messages = messages;
        this.events = events;
        this.scope = scope;
        this.encryption = encryption;
    }

    /** Kept for small unit tests that construct the service without a Spring context. */
    public MessagingService(DirectMessageRepository messages, ApplicationEventPublisher events, RecordScope scope) {
        this(messages, events, scope, new FieldEncryptionService("", "test"));
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> conversation(String clinic, String currentUser, String peer, int limit) {
        if (limit < 1 || limit > 200) throw new BadRequestException("Message limit must be between 1 and 200");
        scope.requireClinic(clinic);
        scope.requirePeerMember(clinic, currentUser);
        requirePeer(clinic, peer, currentUser);
        List<MessageResponse> result = new java.util.ArrayList<>(messages.findConversation(clinic, currentUser, peer, PageRequest.of(0, limit))
                .stream().map(this::response).toList());
        java.util.Collections.reverse(result);
        return result;
    }

    @Transactional
    public MessageResponse send(String clinic, String sender, MessageRequest request) {
        if (request == null || request.recipient() == null || request.body() == null || request.body().isBlank()) {
            throw new BadRequestException("Message recipient and body are required");
        }
        scope.requireClinic(clinic);
        scope.requirePeerMember(clinic, sender);
        String recipient = request.recipient().trim();
        requirePeer(clinic, recipient, sender);
        DirectMessage saved = messages.saveAndFlush(new DirectMessage(clinic, sender, recipient, encryption.encrypt(request.body().trim())));
        MessageResponse response = response(saved);
        events.publishEvent(new DirectMessageCreated(clinic, sender, recipient, response));
        return response;
    }

    @Transactional(readOnly = true)
    public List<com.clincanywhere.dto.MessageContact> contacts(String clinic, String username) {
        scope.requirePeerMember(clinic, username);
        return scope.messageContacts(clinic, username);
    }

    private void requirePeer(String clinic, String peer, String currentUser) {
        if (peer == null || peer.isBlank() || peer.equalsIgnoreCase(currentUser)) {
            throw new BadRequestException("Choose another user to message");
        }
        scope.requireMessagePermission(clinic, currentUser, peer);
    }

    private MessageResponse response(DirectMessage message) {
        return new MessageResponse(message.getId(), message.getClinicId(), message.getSender(), message.getRecipient(),
                encryption.decrypt(message.getBody()), message.getCreatedAt());
    }
}
