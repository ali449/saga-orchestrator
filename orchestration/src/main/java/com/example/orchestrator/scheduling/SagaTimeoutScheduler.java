package com.example.orchestrator.scheduling;

import com.example.common.statics.SagaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Component
public class SagaTimeoutScheduler {
    private static final Logger logger = LoggerFactory.getLogger(SagaTimeoutScheduler.class);
    private static final String TIMEOUT_KEY = "timeout:events";

    //TODO: Move data to another z_set instead of immediate deletion
    private static final String LUA_SCRIPT = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local limit = tonumber(ARGV[2])
            local expired = redis.call('ZRANGEBYSCORE', key, '-inf', now, 'LIMIT', 0, limit)
            if #expired > 0 then
                redis.call('ZREM', key, unpack(expired))
            end
            return expired
            """;

    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<List> luaScript;
    private Consumer<String> callback;

    public SagaTimeoutScheduler(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;

        this.luaScript = new DefaultRedisScript<>();
        this.luaScript.setScriptText(LUA_SCRIPT);
        this.luaScript.setResultType(List.class);
    }

    public void registerTimeoutCallback(Consumer<String> callback) {
        this.callback = callback;
    }

    /**
     * Schedule a new timeout.
     */
    public void scheduleTimeout(String eventId, LocalDateTime expireAt) {
        long delayMillis = getDelayMillis(expireAt);

        if (delayMillis <= 0) {
            throw new IllegalArgumentException(
                    "Cannot schedule timeout in the past. Expires at: " + expireAt
            );
        }

        long expireAtMillis = Instant.now().toEpochMilli() + delayMillis;
        ZSetOperations<String, String> zset = redisTemplate.opsForZSet();
        zset.add(TIMEOUT_KEY, eventId, expireAtMillis);
    }

    /**
     * Cancel timeout if result arrives.
     */
    public void cancelTimeout(String eventId) {
        redisTemplate.opsForZSet().remove(TIMEOUT_KEY, eventId);
    }

    /**
     * Periodically poll for expired events
     */
    @Scheduled(fixedDelay = SagaConstants.TIMEOUT_POLL_INTERVAL_MS)
    public void pollExpiredTimeouts() {
        try {
            long now = Instant.now().toEpochMilli();
            List<String> expiredIds = redisTemplate.execute(
                    luaScript,
                    Collections.singletonList(TIMEOUT_KEY),
                    String.valueOf(now),
                    String.valueOf(SagaConstants.TIMEOUT_POLL_BATCH_SIZE)
            );

            if (expiredIds != null && !expiredIds.isEmpty()) {
                for (String eventId : expiredIds) {
                    handleTimeout(eventId);
                }
            }
        } catch (Exception e) {
            logger.error("Error while polling expired timeouts", e);
        }
    }

    /**
     * What happens when a timeout occurs.
     */
    private void handleTimeout(String eventId) {
        if (callback != null) {
            callback.accept(eventId);
        }
    }

    private static long getDelayMillis(LocalDateTime expireAt) {
        Objects.requireNonNull(expireAt, "ExpireAt cannot be null");

        Instant expireInstant = expireAt.atZone(ZoneId.systemDefault()).toInstant();
        Instant now = Instant.now();

        return Duration.between(now, expireInstant).toMillis();
    }
}
