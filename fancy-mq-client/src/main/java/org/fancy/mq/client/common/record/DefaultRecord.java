package org.fancy.mq.client.common.record;

import org.fancy.mq.client.common.header.Header;
import org.fancy.mq.client.common.utils.ByteUtils;
import org.fancy.mq.client.common.utils.Utils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class DefaultRecord {

    /**
     * 消息记录写入到输出流 out
     * @param offsetDelta
     * @param timestampDelta
     * @param key 消息键值
     * @param value 消息值
     * @param headers 消息头
     * @return 实际的字节数
     */
    public static int writeTo(DataOutputStream out,
                              int offsetDelta,
                              long timestampDelta,
                              ByteBuffer key,
                              ByteBuffer value,
                              Header[] headers) throws IOException {
        int totalSize = sizeInBytes(offsetDelta, timestampDelta, key, value, headers);
        ByteUtils.writeVarint(totalSize, out);

        ByteUtils.writeVarint(offsetDelta, out);
        ByteUtils.writeVarlong(timestampDelta, out);

        if (key == null) {
            ByteUtils.writeVarint(-1, out);
        } else {
            int keyLen = key.remaining(); // 写入的key字节数
            ByteUtils.writeVarint(keyLen, out);
            Utils.writeTo(out, key, keyLen);
        }
        if (value == null) {
            ByteUtils.writeVarint(-1, out);
        } else {
            int valLen = value.remaining(); // 写入的val字节数
            ByteUtils.writeVarint(valLen, out);
            Utils.writeTo(out, value, valLen);
        }

        if (headers == null) {
            throw new IllegalArgumentException("Header cannot null!");
        }
        ByteUtils.writeVarint(headers.length, out);
        for (Header h : headers) {
            String keyStr = h.key();
            if (keyStr == null) {
                throw new IllegalArgumentException("Invalid null header key found in headers");
            }
            byte[] utf8Key = Utils.utf8(keyStr);
            ByteUtils.writeVarint(utf8Key.length, out);
            out.write(utf8Key);

            byte[] headerValue = h.values();
            ByteUtils.writeVarint(headerValue.length, out);
            out.write(headerValue);
        }
        return ByteUtils.sizeOfVarint(totalSize);
    }

    public static int sizeInBytes(int offsetDelta,
                                  long timestampDelta,
                                  ByteBuffer key,
                                  ByteBuffer value,
                                  Header[] headers) {
        int keySize = key == null ? -1 : key.remaining();
        int valueSize = value == null ? -1 : value.remaining();
        int bodySize = sizeOfBodyInBytes(offsetDelta, timestampDelta, keySize, valueSize, headers);
        return ByteUtils.sizeOfVarint(bodySize);
    }

    private static int sizeOfBodyInBytes(int offsetDelta,
                                         long timestampDelta,
                                         int keySize,
                                         int valueSize,
                                         Header[] headers) {
        int size = ByteUtils.sizeOfVarint(offsetDelta);
        size += ByteUtils.sizeOfVarlong(timestampDelta);
        size += sizeOf(keySize, valueSize, headers);
        return size;
    }

    private static int sizeOf(int keySize, int valueSize, Header[] headers) {
        325
       /../..../../.../

        return 0;
    }

}
