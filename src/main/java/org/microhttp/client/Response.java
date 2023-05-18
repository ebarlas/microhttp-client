package org.microhttp.client;

import java.util.List;

public record Response(int status, String reason, List<Header> headers, byte[] body) {}
