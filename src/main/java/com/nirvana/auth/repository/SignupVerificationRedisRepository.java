package com.nirvana.auth.repository;

import com.nirvana.auth.exception.EmailVerificationException;
import com.nirvana.auth.exception.EmailVerificationException.Reason;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SignupVerificationRedisRepository {
    private static final String LEASE_SECONDS = "60";
    private static final String CODE_SECONDS = "300";
    private static final String TOKEN_SECONDS = "1800";
    private static final String RESEND_SECONDS = "60";
    private static final String MAX_ATTEMPTS = "5";

    private static final RedisScript<Long> RESERVE_SEND = script("""
            if redis.call('EXISTS', KEYS[1], KEYS[2]) > 0 then return 0 end
            redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2])
            return 1
            """);
    private static final RedisScript<Long> SAVE_CODE = script("""
            if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
            redis.call('HSET', KEYS[2], 'value', ARGV[2], 'attempts', 0)
            redis.call('EXPIRE', KEYS[2], ARGV[3])
            redis.call('SET', KEYS[3], '1', 'EX', ARGV[4])
            redis.call('DEL', KEYS[1])
            return 1
            """);
    private static final RedisScript<Long> VERIFY_CODE = script("""
            if redis.call('EXISTS', KEYS[1]) > 0 then return 0 end
            local stored = redis.call('HMGET', KEYS[2], 'value', 'attempts')
            if not stored[1] or tonumber(stored[2]) >= tonumber(ARGV[4]) then return 0 end
            if stored[1] ~= ARGV[1] then
                redis.call('HINCRBY', KEYS[2], 'attempts', 1)
                return 0
            end
            redis.call('SET', KEYS[3], ARGV[2], 'EX', ARGV[3])
            redis.call('DEL', KEYS[2])
            return 1
            """);
    private static final RedisScript<Long> RESERVE_TOKEN = script("""
            if redis.call('EXISTS', KEYS[1]) > 0 or redis.call('GET', KEYS[2]) ~= ARGV[1] then
                return 0
            end
            redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3])
            return 1
            """);
    private static final RedisScript<Long> FINISH = script("""
            if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
            if ARGV[2] == 'true' then redis.call('DEL', KEYS[2], KEYS[3]) end
            redis.call('DEL', KEYS[1])
            return 1
            """);

    private final StringRedisTemplate redis;

    public boolean reserveSend(String email, UUID owner) {
        Keys keys = Keys.forEmail(email);
        return run(RESERVE_SEND, List.of(keys.lock(), keys.resend()), owner.toString(), LEASE_SECONDS);
    }

    public void saveCode(String email, UUID owner, String code) {
        Keys keys = Keys.forEmail(email);
        if (!run(SAVE_CODE, List.of(keys.lock(), keys.code(), keys.resend()), owner.toString(),
                code, CODE_SECONDS, RESEND_SECONDS)) {
            throw new EmailVerificationException(Reason.STORAGE_UNAVAILABLE);
        }
    }

    public boolean verifyCode(String email, String code, UUID token) {
        Keys keys = Keys.forEmail(email);
        return run(VERIFY_CODE, List.of(keys.lock(), keys.code(), keys.token()), code, token.toString(),
                TOKEN_SECONDS, MAX_ATTEMPTS);
    }

    public boolean reserveToken(String email, UUID token, UUID owner) {
        Keys keys = Keys.forEmail(email);
        return run(RESERVE_TOKEN, List.of(keys.lock(), keys.token()), token.toString(), owner.toString(), LEASE_SECONDS);
    }

    public void finish(String email, UUID owner, boolean consumed) {
        Keys keys = Keys.forEmail(email);
        run(FINISH, List.of(keys.lock(), keys.token(), keys.code()), owner.toString(), Boolean.toString(consumed));
    }

    private boolean run(RedisScript<Long> script, List<String> keys, String... args) {
        try {
            return Long.valueOf(1).equals(redis.execute(script, keys, (Object[]) args));
        } catch (DataAccessException exception) {
            throw new EmailVerificationException(Reason.STORAGE_UNAVAILABLE, exception);
        }
    }

    private static RedisScript<Long> script(String source) {
        return RedisScript.of(source, Long.class);
    }

    private record Keys(String prefix) {
        static Keys forEmail(String email) {
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    email.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return new Keys("auth:signup:{" + encoded + "}");
        }

        String lock() { return prefix + ":lease"; }
        String resend() { return prefix + ":resend"; }
        String code() { return prefix + ":code"; }
        String token() { return prefix + ":token"; }
    }
}
