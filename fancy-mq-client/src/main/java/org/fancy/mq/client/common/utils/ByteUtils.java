package org.fancy.mq.client.common.utils;

import java.io.DataOutput;
import java.io.IOException;

public class ByteUtils {

    public static int sizeOfVarint(int value) {
        int v = (value << 1) ^ (value >> 31);
        int bytes = 1;
        while ((v & 0xffffff80) != 0) {
            bytes++;
            v >>>= 7;
        }
        return bytes;
    }

    public static int sizeOfVarlong(long value) {
        long v = (value << 1) ^ (value >> 63);
        int bytes = 1;
        while ((v & 0xffffffffffffff80L) != 0L) {
            bytes++;
            v >>>= 7;
        }
        return bytes;
    }

    /**
     * 写int类型的变长字段
     *  65:  0100 0001  -> (1000 0010 ^ 0000 0000) -> 1000 0010
     */
    public static void writeVarint(int value, DataOutput out) throws IOException {
        writeUnsignedVarint((value << 1) ^ (value >> 31), out);
    }

    /**
     * 1000 0010 -> 1000 0010  0000 0001
     */
    private static void writeUnsignedVarint(int value, DataOutput out) throws IOException {
        while ((value & 0xffffff80) != 0) {
            out.writeByte((value & 0x7f) | 0x8f);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    /**
     * 写long类型的变长字段
     */
    public static void writeVarlong(long value, DataOutput out) throws IOException {
        writeUnsignedVarlong((value << 1) ^ (value >> 31), out);
    }

    private static void writeUnsignedVarlong(long value, DataOutput out) throws IOException {
        long v = value;
        while ((v & 0xffffffffffffff80L) != 0) {
            out.writeByte((byte) ((v & 0x7f) | 0x80));
            v >>>= 7;
        }
        out.writeByte((byte)v);
    }
}
