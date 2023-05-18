package org.microhttp.client;

import java.util.ArrayList;
import java.util.List;

class ByteTokenizer {

    private final List<byte[]> buffers = new ArrayList<>();

    private final Iterator cursor = new Iterator(0, 0);
    private int size;

    int size() {
        return size;
    }

    int available() {
        return size - cursor.count;
    }

    void add(byte[] buffer) {
        buffers.add(buffer);
        size += buffer.length;
    }

    byte[] next(byte[] delimiter) {
        var it = find(cursor.copy(), delimiter);
        if (it == null) {
            return null;
        }
        var bytes = fastCopy(it);
        it.next(delimiter.length);
        cursor.add(it);
        return bytes;
    }

    byte[] next(int amount) {
        if (size - cursor.count < amount) {
            return null;
        }
        var it = cursor.copy();
        it.next(amount);
        var bytes = fastCopy(it);
        cursor.add(it);
        return bytes;
    }

    private byte[] fastCopy(Iterator it) {
        var bytes = new byte[it.count];
        var dstPos = 0;
        for (int i = cursor.bufferIndex; i <= it.bufferIndex; i++) {
            if (i < buffers.size()) { // last buffer may be over the edge
                var buf = buffers.get(i);
                var srcStart = i == cursor.bufferIndex ? cursor.byteIndex : 0;
                var srcEnd = i == it.bufferIndex ? it.byteIndex : buf.length;
                var len = srcEnd - srcStart;
                if (len > 0) {
                    System.arraycopy(buf, srcStart, bytes, dstPos, len); // intrinsic candidate
                }
                dstPos += len;
            }
        }
        return bytes;
    }

    private Iterator find(Iterator it, byte[] pattern) {
        while (it.hasNext()) {
            var match = matches(it.copy(), pattern);
            if (match) {
                return it;
            }
            it.next();
        }
        return null;
    }

    private boolean matches(Iterator it, byte[] pattern) {
        while (it.hasNext() && it.count < pattern.length) {
            if (pattern[it.count] != it.next()) {
                return false;
            }
        }
        return it.count == pattern.length;
    }

    private class Iterator {
        int bufferIndex;
        int byteIndex;
        int count;
        byte[] buffer;

        Iterator(int bufferIndex, int byteIndex) {
            this.bufferIndex = bufferIndex;
            this.byteIndex = byteIndex;
            setBuffer();
        }

        Iterator copy() {
            return new Iterator(bufferIndex, byteIndex);
        }

        void add(Iterator it) {
            bufferIndex = it.bufferIndex;
            byteIndex = it.byteIndex;
            count += it.count;
        }

        void setBuffer() {
            this.buffer = bufferIndex < buffers.size()
                    ? buffers.get(bufferIndex)
                    : null;
        }

        boolean hasNext() {
            return bufferIndex < buffers.size();
        }

        byte next() {
            var b = buffer[byteIndex++];
            if (byteIndex == buffer.length) {
                bufferIndex++;
                byteIndex = 0;
                setBuffer();
            }
            count++;
            return b;
        }

        void next(int amount) {
            while (amount > 0) {
                var remaining = buffer.length - byteIndex;
                var len = Math.min(remaining, amount);
                amount -= len;
                byteIndex += len;
                count += len;
                if (byteIndex == buffer.length) {
                    bufferIndex++;
                    byteIndex = 0;
                    setBuffer();
                }
            }
        }
    }

}
