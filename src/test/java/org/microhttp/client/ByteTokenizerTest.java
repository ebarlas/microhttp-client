package org.microhttp.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ByteTokenizerTest {

    @Test
    public void testFragments() {
        var bt = new ByteTokenizer();
        bt.add("GET /res".getBytes());
        bt.add("ource HTTP".getBytes());
        bt.add("/1.1\r\n".getBytes());
        bt.add("Accept: application/json\r\n".getBytes());
        bt.add("Accept-Encoding: gzip\r\n".getBytes());
        Assertions.assertArrayEquals("GET".getBytes(), bt.next(" ".getBytes()));
        Assertions.assertArrayEquals("/resource".getBytes(), bt.next(" ".getBytes()));
        Assertions.assertArrayEquals("HTTP/1.1".getBytes(), bt.next("\r\n".getBytes()));
        Assertions.assertArrayEquals("Accept: application/json".getBytes(), bt.next("\r\n".getBytes()));
        Assertions.assertArrayEquals("Accept-Encoding: gzip".getBytes(), bt.next("\r\n".getBytes()));
        Assertions.assertNull(bt.next("\r\n".getBytes()));
    }
    
    @Test
    public void testSimple() {
        var bt = new ByteTokenizer();
        bt.add("hello world".getBytes());
        Assertions.assertArrayEquals("he".getBytes(), bt.next(2));
        Assertions.assertArrayEquals("ll".getBytes(), bt.next(2));
        Assertions.assertArrayEquals("o ".getBytes(), bt.next(2));
        Assertions.assertArrayEquals("wo".getBytes(), bt.next(2));
        Assertions.assertNull(bt.next(5));
    }

}
