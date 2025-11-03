package com.example.common.messaging;

import com.example.common.messaging.model.BaseEvent;

public interface EventHandler<T extends BaseEvent> {
    void onEvent(T event);
}

