package org.microhttp.client;

import java.io.IOException;
import java.net.Socket;

public record Connection(
        HostPort localAddress,
        HostPort remoteAddress,
        Socket socket,
        long createTime,
        long lastUseTime,
        int borrowCounter) {

    Connection(HostPort localAddress, HostPort remoteAddress, Socket socket) {
        this(localAddress, remoteAddress, socket, System.nanoTime(), 0, 1);
    }

    Connection use() {
        return new Connection(localAddress, remoteAddress, socket, createTime, System.nanoTime(), borrowCounter);
    }

    Connection borrow() {
        return new Connection(localAddress, remoteAddress, socket, createTime, lastUseTime, borrowCounter + 1);
    }

    void tryClose() {
        try {
            socket.close();
        } catch (IOException ignore) {}
    }

}
