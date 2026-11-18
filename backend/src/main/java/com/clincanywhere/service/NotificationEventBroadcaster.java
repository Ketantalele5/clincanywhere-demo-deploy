package com.clincanywhere.service;

import com.clincanywhere.dto.NotificationResponse;

public interface NotificationEventBroadcaster {
    void publish(NotificationResponse event);
}
