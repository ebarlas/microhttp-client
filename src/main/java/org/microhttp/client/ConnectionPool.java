package org.microhttp.client;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReentrantLock;

class ConnectionPool {

    private final SocketFactory socketFactory;
    private final HostPort localAddress;
    private final HostPort remoteAddress;
    private final int socketTimeout;
    private final int idleTimeout;
    private final int evictPollPeriod;
    private final ThreadFactory threadFactory;
    private final ReentrantLock lock;
    private final Queue<Connection> connections;

    public ConnectionPool(
            SocketFactory socketFactory,
            HostPort localAddress,
            HostPort remoteAddress,
            int socketTimeout,
            int idleTimeout,
            int evictPollPeriod,
            ThreadFactory threadFactory) {
        this.socketFactory = socketFactory;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.socketTimeout = socketTimeout;
        this.idleTimeout = idleTimeout;
        this.evictPollPeriod = evictPollPeriod;
        this.threadFactory = threadFactory;
        lock = new ReentrantLock();
        connections = new LinkedList<>();
    }

    void start() {
        threadFactory.newThread(this::runEvictionTask).start();
    }

    Connection borrow() throws IOException {
        lock.lock();
        try {
            var c = connections.poll();
            if (c != null) {
                return c.borrow();
            }
        } finally {
            lock.unlock();
        }
        return new Connection(localAddress, remoteAddress, newSocket());
    }

    void release(Connection connection) {
        if (!connection.socket().isClosed()) { // discard closed connection, it can become unreachable
            lock.lock();
            try {
                connections.add(connection.use());
            } finally {
                lock.unlock();
            }
        }
    }

    private Socket newSocket() throws IOException {
        var socket = socketFactory.createSocket();
        socket.bind(localAddress == null ? null : localAddress.toSocketAddress());
        socket.connect(remoteAddress.toSocketAddress(), socketTimeout);
        if (socket instanceof SSLSocket ss) {
            ss.startHandshake(); // complete TLS handshake as part of init
        }
        return socket;
    }

    private void runEvictionTask() {
        while (true) {
            purgeExpired();
            try {
                Thread.sleep(evictPollPeriod);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void purgeExpired() {
        lock.lock();
        try {
            Connection c;
            while ((c = connections.peek()) != null && System.nanoTime() > c.lastUseTime() + idleTimeout) {
                connections.poll();
                c.tryClose();
            }
        } finally {
            lock.unlock();
        }
    }

}
