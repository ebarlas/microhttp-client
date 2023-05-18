package org.microhttp.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ResponseParserTest {

    @Test
    public void testParseNoBody() {
        var response = """
                HTTP/1.1 204 \r
                date: Wed, 17 May 2023 16:07:46 GMT\r
                \r
                """;
        var bt = new ByteTokenizer();
        bt.add(response.getBytes());
        var rp = new ResponseParser(bt);
        Assertions.assertTrue(rp.parse());
        var res = rp.response();
        Assertions.assertEquals(List.of(new Header("date", "Wed, 17 May 2023 16:07:46 GMT")), res.headers());
        Assertions.assertEquals(204, res.status());
        Assertions.assertEquals("", res.reason());
        Assertions.assertArrayEquals(new byte[0], res.body());
    }

    @Test
    public void testParseWithBody() {
        var response = """
                HTTP/1.1 200 OK\r
                date: Wed, 17 May 2023 16:07:46 GMT\r
                content-type: text/plain\r
                content-length: 11\r
                \r
                hello world""";
        var bt = new ByteTokenizer();
        bt.add(response.getBytes());
        var rp = new ResponseParser(bt);
        Assertions.assertTrue(rp.parse());
        var res = rp.response();
        var expected = List.of(
                new Header("date", "Wed, 17 May 2023 16:07:46 GMT"),
                new Header("content-type", "text/plain"),
                new Header("content-length", "11"));
        Assertions.assertEquals(expected, res.headers());
        Assertions.assertEquals(200, res.status());
        Assertions.assertEquals("OK", res.reason());
        Assertions.assertArrayEquals("hello world".getBytes(), res.body());
    }

}
