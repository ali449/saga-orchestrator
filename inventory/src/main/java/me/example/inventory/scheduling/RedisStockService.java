package me.example.inventory.scheduling;

import com.example.common.statics.SagaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisStockService {
    private static final Logger logger = LoggerFactory.getLogger(RedisStockService.class);
    private static final String STOCK_KEY = "stock:";
    private static final String RESERVATIONS_KEY = "reservations:";
    private static final String RESERVATION_KEY = "reservation:";
    private static final String ACTIVE_RESERVATIONS_KEY = "active_reservations";
    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<String> reserveStockScript;
    private final DefaultRedisScript<String> releaseStockScript;
    private final DefaultRedisScript<List> cleanupAllExpiredScript;

    public RedisStockService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;

        this.reserveStockScript = new DefaultRedisScript<>();
        this.reserveStockScript.setLocation(new ClassPathResource("scripts/reserve-stock.lua"));
        this.reserveStockScript.setResultType(String.class);

        this.releaseStockScript = new DefaultRedisScript<>();
        this.releaseStockScript.setLocation(new ClassPathResource("scripts/release-stock.lua"));
        this.releaseStockScript.setResultType(String.class);

        this.cleanupAllExpiredScript = new DefaultRedisScript<>();
        this.cleanupAllExpiredScript.setLocation(new ClassPathResource("scripts/expire-reservations.lua"));
        this.cleanupAllExpiredScript.setResultType(List.class);
    }

    public boolean reserveStock(String aggregateId, String stockId, int quantity) {
        int ttlSeconds = SagaConstants.RELEASE_RESOURCE_TIMEOUT_MS / 1000;
        List<String> keys = List.of(
                stockKey(stockId),
                reservationsKey(stockId),
                ACTIVE_RESERVATIONS_KEY,
                reservationKey(aggregateId)
        );
        long now = System.currentTimeMillis() / 1000;
        String result = redisTemplate.execute(
                reserveStockScript,
                keys,
                aggregateId,
                String.valueOf(quantity), String.valueOf(now), String.valueOf(ttlSeconds), stockId

        );
        logger.info("Result of reserving is {} ", result);
        return result != null && result.startsWith("RESERVED");
    }

    public boolean releaseStock(String aggregateId, String stockId) {
        List<String> keys = List.of(
                stockKey(stockId),
                reservationsKey(stockId),
                ACTIVE_RESERVATIONS_KEY,
                reservationKey(aggregateId)
        );

        String result = redisTemplate.execute(
                releaseStockScript,
                keys,
                stockId, aggregateId
        );

        logger.info("Release result: {}", result);
        return result != null && result.startsWith("RELEASED");
    }

    public void deleteFromActiveReservations(String stockId) {
        Long removedCount = redisTemplate.opsForSet().remove(ACTIVE_RESERVATIONS_KEY, stockId);
        if (removedCount != null && removedCount <= 0) {
            logger.warn("Value not found in set, nothing removed");
        }
    }

    @Scheduled(fixedDelay = SagaConstants.RELEASE_RESOURCE_SCHEDULER_MS)
    public void cleanupExpired() {
        long now = System.currentTimeMillis() / 1000;
        int limit = 100;

        List<String> keys = List.of(
                STOCK_KEY,
                RESERVATIONS_KEY,
                ACTIVE_RESERVATIONS_KEY
        );
        List<String> expiredProducts = redisTemplate.execute(
                cleanupAllExpiredScript,
                keys,
                String.valueOf(now), String.valueOf(limit)
        );

        if (expiredProducts != null && !expiredProducts.isEmpty()) {
            logger.info("Expired product reservations: {}", expiredProducts);
            // Optionally trigger DB sync or Kafka event
        }
    }

    private static String stockKey(String stockId) {
        return STOCK_KEY + stockId;
    }

    private static String reservationsKey(String stockId) {
        return RESERVATIONS_KEY + stockId;
    }

    private static String reservationKey(String aggregateId) {
        return RESERVATION_KEY + aggregateId;
    }

    public long increaseStockAmount(String stockId, int amount) {
        Long newStock = redisTemplate.opsForValue().increment(stockKey(stockId), amount);
        return newStock != null ? newStock : 0L;
    }

    public int getStockAmount(String stockId) {
        String quantityStr = redisTemplate.opsForValue().get(stockKey(stockId));
        if (quantityStr != null) {
            return Integer.parseInt(quantityStr);
        }
        return 0;
    }

    public boolean clearStockAmount(String stockId) {
        Boolean isReserved = redisTemplate.opsForSet().isMember(ACTIVE_RESERVATIONS_KEY, stockId);
        if (Boolean.TRUE.equals(isReserved)) {
            logger.info("Cannot clear stock amount because reservation");
            return false;
        }
        Boolean result = redisTemplate.delete(stockKey(stockId));
        return Boolean.TRUE.equals(result);
    }
}
