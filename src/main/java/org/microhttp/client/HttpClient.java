package org.microhttp.client;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class HttpClient {

    private final int bufferSize;

    public HttpClient(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    Response send(Connection connection, Request request) throws IOException {
        var socket = connection.socket();
        try {
            return send(socket, request);
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }

    Response send(Socket socket, Request request) throws IOException {
        var os = socket.getOutputStream();
        var is = socket.getInputStream();
        os.write(RequestSerializer.serialize(request));
        var buffer = new byte[bufferSize];
        var bt = new ByteTokenizer();
        var rp = new ResponseParser(bt);
        while (!rp.parse()) {
            var n = is.read(buffer, 0, bufferSize);
            if (n < 0) {
                throw new EOFException();
            }
            bt.add(Arrays.copyOfRange(buffer, 0, n));
        }
        return rp.response();
    }
}
