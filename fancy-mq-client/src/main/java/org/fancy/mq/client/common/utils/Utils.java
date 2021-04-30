package org.fancy.mq.client.common.utils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Utils {

    private Utils() {
    }

    public static int toPositive(int number) {
        return number & Integer.MAX_VALUE;
    }

    public static void writeTo(DataOutputStream out, ByteBuffer key, int keyLen) throws IOException {
        if (key.hasArray()) {
            out.write(key.array(), key.position() + key.arrayOffset(), keyLen);
        } else {
            for (int i = key.position(); i < key.position() + keyLen; i++) {
                out.writeByte(key.get(i));
            }
        }
    }

    /**
     * 字符串转utf8字节数组
     */
    public static byte[] utf8(String keyStr) {
        return keyStr.getBytes(StandardCharsets.UTF_8);
    }

    public static ByteBuffer wrapperNullable(byte[] key) {
        return key == null ? null : ByteBuffer.wrap(key);
    }
}
