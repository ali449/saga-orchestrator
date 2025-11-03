package com.example.common.utils;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Try<T> {
    private final T value;
    private final Exception exception;

    private Try(T value, Exception exception) {
        this.value = value;
        this.exception = exception;
    }

    public static <T> Try<T> of(Supplier<T> supplier) {
        try {
            return new Try<>(supplier.get(), null);
        } catch (Exception e) {
            return new Try<>(null, e);
        }
    }

    public static Try<Void> run(Runnable runnable) {
        try {
            runnable.run();
            return new Try<>(null, null);
        } catch (Exception e) {
            return new Try<>(null, e);
        }
    }

    public boolean isSuccess() {
        return exception == null;
    }

    public boolean isFailure() {
        return exception != null;
    }

    public Try<T> onFailure(Consumer<Exception> consumer) {
        if (isFailure()) {
            consumer.accept(exception);
        }
        return this;
    }

    public Try<T> onSuccess(Consumer<T> consumer) {
        if (isSuccess()) {
            consumer.accept(value);
        }
        return this;
    }

    public T getOrElse(T defaultValue) {
        return isSuccess() ? value : defaultValue;
    }

    public T getOrNull() {
        return value;
    }

    public Exception getException() {
        return exception;
    }
}



