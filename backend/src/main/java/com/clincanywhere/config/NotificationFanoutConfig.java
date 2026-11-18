package com.clincanywhere.config;

import com.clincanywhere.dto.NotificationResponse;
import com.clincanywhere.service.NotificationEventBroadcaster;
import com.clincanywhere.service.NotificationService;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.realtime.shared-events.enabled", havingValue = "true")
public class NotificationFanoutConfig {
    public static final String EXCHANGE = "clincanywhere.notifications";

    @Bean FanoutExchange notificationFanoutExchange() { return new FanoutExchange(EXCHANGE, true, false); }
    @Bean Queue notificationFanoutQueue() { return new AnonymousQueue(); }
    @Bean Binding notificationFanoutBinding(Queue notificationFanoutQueue, FanoutExchange notificationFanoutExchange) {
        return BindingBuilder.bind(notificationFanoutQueue).to(notificationFanoutExchange);
    }
    @Bean JacksonJsonMessageConverter rabbitJsonMessageConverter() { return new JacksonJsonMessageConverter(); }

    @Bean NotificationEventBroadcaster notificationEventBroadcaster(RabbitTemplate rabbitTemplate,
                                                                     NotificationService notifications) {
            return new RabbitNotificationEventBroadcaster(rabbitTemplate, notifications);
    }

    static final class RabbitNotificationEventBroadcaster implements NotificationEventBroadcaster {
        private final RabbitTemplate rabbitTemplate;
        private final NotificationService notifications;

        RabbitNotificationEventBroadcaster(RabbitTemplate rabbitTemplate, NotificationService notifications) {
            this.rabbitTemplate = rabbitTemplate;
            this.notifications = notifications;
        }

        @Override public void publish(NotificationResponse event) {
            rabbitTemplate.convertAndSend(EXCHANGE, "", event);
        }

        @RabbitListener(queues = "#{@notificationFanoutQueue.name}")
        public void receive(NotificationResponse event) {
            notifications.deliverToLocalSubscribers(event);
        }
    }
}
