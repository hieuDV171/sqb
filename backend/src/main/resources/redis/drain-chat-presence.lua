local now, limit = tonumber(ARGV[1]), tonumber(ARGV[2])
local expired = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', now, 'LIMIT', 0, limit)
for _, user in ipairs(expired) do
    redis.call('ZREM', KEYS[1], user)
    redis.call('SADD', KEYS[2], user)
end
return redis.call('SPOP', KEYS[2], limit)
