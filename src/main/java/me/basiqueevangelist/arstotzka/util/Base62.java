package me.basiqueevangelist.arstotzka.util;

import com.google.common.math.IntMath;
import com.google.common.math.LongMath;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

// A nice base62 id generator that generates tokens much like modrinth's.
public final class Base62 {
    private static final byte[] ALPHABET =
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.UTF_8);

    private Base62() {

    }

    @SuppressWarnings("UnstableApiUsage")
    public static String random(int len) {
        long min = LongMath.saturatedPow(62, len - 1);
        long max = LongMath.saturatedPow(62, len);

        return toBase62(ThreadLocalRandom.current().nextLong(min, max));
    }

    public static String toBase62(long l) {
        byte[] chars = new byte[(int) Math.ceil(Math.log(l) / Math.log(62))];
        int i = chars.length - 1;

        while (l > 0) {
            chars[i] = ALPHABET[(int) (l % 62)];
            l /= 62;
            i--;
        }

        return new String(chars, StandardCharsets.UTF_8);
    }
}
