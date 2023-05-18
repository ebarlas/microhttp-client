package org.microhttp.client;

import java.util.List;

public record Request(String method, String uri, List<Header> headers, byte[] body) {}