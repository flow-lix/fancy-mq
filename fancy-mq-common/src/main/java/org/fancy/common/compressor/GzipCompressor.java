package org.fancy.common.compressor;

import org.fancy.common.util.GzipUtil;

public class GzipCompressor implements Compressor {

    @Override
    public byte[] compress(byte[] bytes) {
        return GzipUtil.compress(bytes);
    }

    @Override
    public byte[] decompress(byte[] bytes) {
        return GzipUtil.decompress(bytes);
    }
}
