package org.fancy.mq.client.common.record;

import org.fancy.common.compressor.CompressionType;
import org.fancy.mq.client.common.exception.KafkaException;
import org.fancy.mq.client.common.header.Header;
import org.fancy.mq.client.common.utils.Utils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class MemoryRecordBuilder {

    private static final float COMPRESSION_RATE_ESTIMATION_FACTOR = 1.05f;

    private static final DataOutputStream CLOSED_OUT_STREAM = new DataOutputStream(new OutputStream() {
        @Override
        public void write(int b) throws IOException {
            throw new IllegalStateException("MemoryRecordBuilder已关闭, 禁止写入数据");
        }
    });

    private final CompressionType compressionType;

    private DataOutputStream appendStream;
    private int numRecords = 0;

    private final long baseOffset;

    private Long lastOffset = null;
    private Long firstTimestamp = null;

    private final int writeLimit;
    private final int batchHeaderSizeInBytes;

    private float estimatedCompressionRatio = 1.0F;

    private int uncompressedRecordsSizeInBytes;

    public void append(long timestamp, byte[] key, byte[] value, Header[] headers) {
        append(nextSequentialOffset(), timestamp, key, value, headers);
    }

    private void append(long offset, long timestamp, byte[] key, byte[] value, Header[] headers) {
        try {
            if (lastOffset != null && offset <= lastOffset) {
                throw new IllegalArgumentException(String.format("Illegal offset %s following previous offset %s " +
                        "(Offsets must increase monotonically).", offset, lastOffset));
            }
            if (timestamp < 0 && timestamp != RecordBatch.NO_TIMESTAMP) {
                throw new IllegalArgumentException("Invalid negative timestamp " + timestamp);
            }
            if (firstTimestamp == null) {
                firstTimestamp = timestamp;
            }
            appendDefaultRecord(offset, timestamp, Utils.wrapperNullable(key), Utils.wrapperNullable(value), headers);
        } catch (Exception e) {
            throw new IllegalStateException("写入到追加流时发生了IOException", e);
        }
    }

    private void appendDefaultRecord(long offset, long timestamp, ByteBuffer key, ByteBuffer value,
                                     Header[] headers) throws IOException {
        ensureOpenForRecordAppend();
        int offsetDelta = (int) (offset - lastOffset);
        long timestampDelta = timestamp - firstTimestamp;
        DefaultRecord.writeTo(appendStream, offsetDelta, timestampDelta, key, value, headers);
    }

    private void ensureOpenForRecordAppend() {
        if (appendStream != CLOSED_OUT_STREAM) {
            throw new IllegalStateException("尝试追加记录时，MemoryRecordBuilder 已经关闭");
        }
    }

    private long nextSequentialOffset() {
        return lastOffset == null ? baseOffset : lastOffset + 1;
    }

    public void closeForRecordAppends() {
        if (appendStream != CLOSED_OUT_STREAM) {
            try {
                appendStream.close();
            } catch (Exception e) {
                throw new KafkaException(e);
            } finally {
                appendStream = CLOSED_OUT_STREAM;
            }
        }
    }

    public boolean isFull() {
        return appendStream == CLOSED_OUT_STREAM || (this.numRecords > 0 && writeLimit <= estimatedBytesWritten());
    }

    /**
     * Get an estimate of the number of bytes written (based on the estimation factor hard-coded in {@link CompressionType}.
     * @return The estimated number of bytes written
     */
    private int estimatedBytesWritten() {
        if (compressionType == CompressionType.NONE) {
            return batchHeaderSizeInBytes + uncompressedRecordsSizeInBytes;
        } else {
            // estimate the written bytes to the underlying byte buffer based on uncompressed written bytes
            return batchHeaderSizeInBytes + (int) (uncompressedRecordsSizeInBytes * estimatedCompressionRatio * COMPRESSION_RATE_ESTIMATION_FACTOR);
        }
    }
}
