package org.microhttp.client;

import java.util.ArrayList;
import java.util.List;

class ByteMerger {

    private final List<byte[]> arrays = new ArrayList<>();

    void add(byte[] array) {
        arrays.add(array);
    }

    byte[] merge() {
        var size = arrays.stream().mapToInt(a -> a.length).sum();
        var result = new byte[size];
        var offset = 0;
        for (var array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

}
