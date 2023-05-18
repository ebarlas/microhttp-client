package org.microhttp.client;

import java.net.InetSocketAddress;

public record HostPort(String host, int port) {

    InetSocketAddress toSocketAddress() {
        return new InetSocketAddress(host, port);
    }

}
