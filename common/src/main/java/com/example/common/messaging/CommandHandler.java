package com.example.common.messaging;

import com.example.common.messaging.model.BaseCommand;

public interface CommandHandler<T extends BaseCommand> {
    void handle(T command);
}

