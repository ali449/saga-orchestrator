package com.example.orchestrator.dto;

import com.example.common.messaging.model.BaseEvent;
import com.example.common.messaging.model.SagaStatusMessage;

public record SagaExecuteResult(boolean success, String sagaInstanceId, String message, OutBox outBox,
                                SagaStatusMessage status, BaseEvent triggeringEvent) {
    public static SagaExecuteResultBuilder successBuilder() {
        return new SagaExecuteResultBuilder(true);
    }

    public static SagaExecuteResultBuilder failBuilder() {
        return new SagaExecuteResultBuilder(false);
    }

    public static final class SagaExecuteResultBuilder {
        private boolean success;
        private String sagaInstanceId;
        private String message;
        private OutBox outBox;
        private SagaStatusMessage status;
        private BaseEvent triggeringEvent;

        public SagaExecuteResult build() {
            return new SagaExecuteResult(success, sagaInstanceId, message, outBox, status, triggeringEvent);
        }

        public SagaExecuteResultBuilder(boolean success) {
            this.success = success;
        }

        public SagaExecuteResultBuilder sagaInstanceId(String sagaInstanceId) {
            this.sagaInstanceId = sagaInstanceId;
            return this;
        }

        public SagaExecuteResultBuilder message(String message) {
            this.message = message;
            return this;
        }

        public SagaExecuteResultBuilder outBox(OutBox outBox) {
            this.outBox = outBox;
            return this;
        }

        public SagaExecuteResultBuilder status(SagaStatusMessage status) {
            this.status = status;
            return this;
        }

        public SagaExecuteResultBuilder triggeringEvent(BaseEvent triggeringEvent) {
            this.triggeringEvent = triggeringEvent;
            return this;
        }
    }
}
