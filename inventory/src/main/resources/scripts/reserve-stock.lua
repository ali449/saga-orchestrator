-- KEYS[1] = stock:{productId}
-- KEYS[2] = reservations:{productId}
-- KEYS[3] = active_reservations
-- KEYS[4] = reservation:{orderId}
-- ARGV[1] = orderId
-- ARGV[2] = quantity
-- ARGV[3] = now
-- ARGV[4] = ttlSeconds
-- ARGV[5] = productId

local orderId = ARGV[1]
local qty = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local ttl = tonumber(ARGV[4])
local productId = ARGV[5]

local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
if stock < qty then
    return "INSUFFICIENT_STOCK"
end

local expireAt = now + ttl

redis.call('DECRBY', KEYS[1], qty)
redis.call('ZADD', KEYS[2], expireAt, orderId)
redis.call('SADD', KEYS[3], productId)
redis.call('HSET', KEYS[4], 'qty', qty, 'productId', productId, 'expireAt', expireAt)

return "RESERVED:" .. expireAt
