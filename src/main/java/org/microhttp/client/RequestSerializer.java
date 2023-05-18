package org.microhttp.client;

class RequestSerializer {

    static final byte[] COLON_SPACE = ": ".getBytes();
    static final byte[] SPACE = " ".getBytes();
    static final byte[] CRLF = "\r\n".getBytes();
    static final byte[] VERSION = "HTTP/1.1".getBytes();

    static byte[] serialize(Request request) {
        var merger = new ByteMerger();
        merger.add(request.method().getBytes());
        merger.add(SPACE);
        merger.add(request.uri().getBytes());
        merger.add(SPACE);
        merger.add(VERSION);
        merger.add(CRLF);
        for (var header : request.headers()) {
            merger.add(header.name().getBytes());
            merger.add(COLON_SPACE);
            merger.add(header.value().getBytes());
            merger.add(CRLF);
        }
        merger.add(CRLF);
        merger.add(request.body());
        return merger.merge();
    }

}
