-- KEYS[1] = stock:
-- KEYS[2] = reservations:
-- KEYS[3] = active_reservations
-- ARGV[1] = now
-- ARGV[2] = limit

local now = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
local expiredProducts = {}

local products = redis.call('SMEMBERS', KEYS[3])
for _, productId in ipairs(products) do
    local resKey = KEYS[2] .. productId
    local stockKey = KEYS[1] .. productId

    local expiredOrders = redis.call('ZRANGEBYSCORE', resKey, '-inf', now)
    local totalRestored = 0

    for _, orderId in ipairs(expiredOrders) do
        local resHash = "reservation:" .. orderId
        local qtyStr = redis.call('HGET', resHash, 'qty')
        local qty = tonumber(qtyStr or '0')

        if qty > 0 then
            totalRestored = totalRestored + qty
        end

        redis.call('ZREM', resKey, orderId)
        redis.call('DEL', resHash)
    end

    if totalRestored > 0 then
        redis.call('INCRBY', stockKey, totalRestored)
        table.insert(expiredProducts, productId)
    end

    if redis.call('ZCARD', resKey) == 0 then
        redis.call('SREM', KEYS[3], productId)
    end

    if #expiredProducts >= limit then
        break
    end
end

return expiredProducts
