-- KEYS[1] = stock:{productId}
-- KEYS[2] = reservations:{productId}
-- KEYS[3] = active_reservations
-- KEYS[4] = reservation:{orderId}
-- ARGV[1] = productId
-- ARGV[2] = orderId

local productId = ARGV[1]
local orderId = ARGV[2]

-- Read qty from reservation hash
local qtyStr = redis.call('HGET', KEYS[4], 'qty')
local qty = tonumber(qtyStr or '0')

if qty > 0 then
    redis.call('INCRBY', KEYS[1], qty)
    redis.call('ZREM', KEYS[2], orderId)
    redis.call('DEL', KEYS[4])
end

-- Clean active reservation if none remain
if redis.call('ZCARD', KEYS[2]) == 0 then
    redis.call('SREM', KEYS[3], productId)
end

return "RELEASED:" .. tostring(qty)
