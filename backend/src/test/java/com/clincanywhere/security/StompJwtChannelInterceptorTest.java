package com.clincanywhere.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

@SuppressWarnings("null") // Mockito-generated doubles are passed to Spring's non-null API boundaries.
class StompJwtChannelInterceptorTest {
    @Test
    void rejectsClientPublishingToBrokerQueues() {
        StompJwtChannelInterceptor interceptor = new StompJwtChannelInterceptor(
                mock(JwtService.class), mock(AccountUserDetailsService.class));
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/queue/messages");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(message, mock(MessageChannel.class)));
    }
}
