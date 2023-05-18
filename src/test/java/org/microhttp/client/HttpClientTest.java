package org.microhttp.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;

public class HttpClientTest {

    @Test
    public void testGitHubContent() throws IOException {
        var remoteAddress = new HostPort("raw.githubusercontent.com", 443);
        var cp = new ConnectionPool(
                SSLSocketFactory.getDefault(),
                null,
                remoteAddress,
                5_000,
                5_000,
                10_000,
                1_000,
                Thread::new);
        var client = new HttpClient(4_096);
        var request = new Request(
                "GET",
                "/ebarlas/microhttp/main/.gitignore",
                List.of(
                        new Header("Host", "raw.githubusercontent.com"),
                        new Header("Accept", "*/*")),
                new byte[0]);
        var connection = cp.borrow();
        Assertions.assertEquals(1, connection.borrowCounter());
        Assertions.assertNull(connection.localAddress());
        Assertions.assertEquals(remoteAddress, connection.remoteAddress());
        Assertions.assertInstanceOf(SSLSocket.class, connection.socket());
        Assertions.assertTrue(connection.socket().isConnected());
        try {
            var response = client.send(connection, request);
            Assertions.assertEquals(200, response.status());
            Assertions.assertTrue(new String(response.body()).contains("target/"));
        } finally {
            cp.release(connection);
        }
    }

    @Test
    public void testSocketConnectTimeout() {
        var remoteAddress = new HostPort("raw.githubusercontent.com", 443);
        var cp = new ConnectionPool(
                SSLSocketFactory.getDefault(),
                null,
                remoteAddress,
                1,
                5_000,
                10_000,
                1_000,
                Thread::new);
        Assertions.assertThrows(SocketTimeoutException.class, cp::borrow, "Connect timed out");
    }

    @Test
    public void testSocketReadTimeout() {
        var remoteAddress = new HostPort("raw.githubusercontent.com", 443);
        var cp = new ConnectionPool(
                SSLSocketFactory.getDefault(),
                null,
                remoteAddress,
                5_000,
                1,
                10_000,
                1_000,
                Thread::new);
        Assertions.assertThrows(SocketTimeoutException.class, cp::borrow, "Read timed out");
    }

}
