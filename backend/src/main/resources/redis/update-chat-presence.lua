local user, socket, now, duration, active = ARGV[1], ARGV[2], tonumber(ARGV[3]), tonumber(ARGV[4]), ARGV[5] == 'true'
local oldDeadline = tonumber(redis.call('ZSCORE', KEYS[2], user))

redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', now)
if active then
    redis.call('ZADD', KEYS[1], now + duration, socket)
else
    -- A close or blur can shorten an existing lease, but cannot create one.
    redis.call('ZADD', KEYS[1], 'XX', 'LT', now + duration, socket)
end

local last = redis.call('ZREVRANGE', KEYS[1], 0, 0, 'WITHSCORES')
if #last > 0 then
    redis.call('PEXPIREAT', KEYS[1], last[2])
    redis.call('ZADD', KEYS[2], last[2], user)
    if not oldDeadline or oldDeadline <= now then
        redis.call('SADD', KEYS[3], user)
    end
end
return 1
