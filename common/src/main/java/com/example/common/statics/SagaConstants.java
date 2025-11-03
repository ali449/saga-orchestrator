package com.example.common.statics;

public abstract class SagaConstants {
    public static final int RETRY_THRESHOLD = 3;
    public static final int RETRY_INTERVAL = 1_000;

    public static final int REQUEST_TIMEOUT_MS = 30_000;
    public static final int DELIVERY_TIMEOUT_MS = 120_000;

    public static final int TIMEOUT_POLL_INTERVAL_MS = 2_000;
    public static final int TIMEOUT_POLL_BATCH_SIZE = 500;

    public static final int TOTAL_WORKFLOW_TIMEOUT_MS = 60_000;
    public static final int RELEASE_RESOURCE_TIMEOUT_MS =
            TOTAL_WORKFLOW_TIMEOUT_MS + (RETRY_INTERVAL * RETRY_THRESHOLD) + (TOTAL_WORKFLOW_TIMEOUT_MS / 2);
    public static final int RELEASE_RESOURCE_SCHEDULER_MS = (TOTAL_WORKFLOW_TIMEOUT_MS / 3);
}
