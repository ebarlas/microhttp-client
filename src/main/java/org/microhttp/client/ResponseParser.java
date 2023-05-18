package org.microhttp.client;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.Function;

class ResponseParser {

    private static final byte[] CRLF = "\r\n".getBytes();
    private static final byte[] SPACE = " ".getBytes();

    private static final String HEADER_CONTENT_LENGTH = "Content-Length";
    private static final String HEADER_TRANSFER_ENCODING = "Transfer-Encoding";
    private static final String CHUNKED = "chunked";

    private static final int RADIX_HEX = 16;

    enum State {
        VERSION(p -> p.tokenizer.next(SPACE), ResponseParser::parseVersion),
        STATUS_CODE(p -> p.tokenizer.next(SPACE), ResponseParser::parseStatusCode),
        REASON_PHRASE(p -> p.tokenizer.next(CRLF), ResponseParser::parseReasonPhrase),
        HEADER(p -> p.tokenizer.next(CRLF), ResponseParser::parseHeader),
        BODY(p -> p.tokenizer.next(p.contentLength), ResponseParser::parseBody),
        CHUNK_SIZE(p -> p.tokenizer.next(CRLF), ResponseParser::parseChunkSize),
        CHUNK_DATA(p -> p.tokenizer.next(p.chunkSize), ResponseParser::parseChunkData),
        CHUNK_DATA_END(p -> p.tokenizer.next(CRLF), (rp, token) -> rp.parseChunkDateEnd()),
        CHUNK_TRAILER(p -> p.tokenizer.next(CRLF), (rp, token) -> rp.parseChunkTrailer()),
        DONE(null, null);

        final Function<ResponseParser, byte[]> tokenSupplier;
        final BiConsumer<ResponseParser, byte[]> tokenConsumer;

        State(Function<ResponseParser, byte[]> tokenSupplier, BiConsumer<ResponseParser, byte[]> tokenConsumer) {
            this.tokenSupplier = tokenSupplier;
            this.tokenConsumer = tokenConsumer;
        }
    }

    private final ByteTokenizer tokenizer;

    private State state = State.VERSION;
    private int contentLength;
    private int chunkSize;
    private ByteMerger chunks = new ByteMerger();

    private String version;
    private int statusCode;
    private String reasonPhrase;
    private List<Header> headers = new ArrayList<>();
    private byte[] body = new byte[0];

    ResponseParser(ByteTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    boolean parse() {
        while (state != State.DONE) {
            byte[] token = state.tokenSupplier.apply(this);
            if (token == null) {
                return false;
            }
            state.tokenConsumer.accept(this, token);
        }
        return true;
    }

    Response response() {
        return new Response(statusCode, reasonPhrase, headers, body);
    }

    private void parseVersion(byte[] token) {
        version = new String(token);
        state = State.STATUS_CODE;
    }

    private void parseStatusCode(byte[] token) {
        try {
            statusCode = Integer.parseInt(new String(token));
            state = State.REASON_PHRASE;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("invalid status code");
        }
    }

    private void parseReasonPhrase(byte[] token) {
        reasonPhrase = new String(token);
        state = State.HEADER;
    }

    private void parseHeader(byte[] token) {
        if (token.length == 0) { // CR-LF on own line, end of headers
            var contentLength = findContentLength();
            if (contentLength.isEmpty()) {
                if (hasChunkedEncodingHeader()) {
                    state = State.CHUNK_SIZE;
                } else {
                    state = State.DONE;
                }
            } else {
                this.contentLength = contentLength.getAsInt();
                state = State.BODY;
            }
        } else {
            headers.add(parseHeaderLine(token));
        }
    }

    private static Header parseHeaderLine(byte[] line) {
        int colonIndex = indexOfColon(line);
        if (colonIndex <= 0) {
            throw new IllegalStateException("malformed header line");
        }
        int spaceIndex = colonIndex + 1;
        while (spaceIndex < line.length && line[spaceIndex] == ' ') { // advance beyond variable-length space prefix
            spaceIndex++;
        }
        return new Header(
                new String(line, 0, colonIndex),
                new String(line, spaceIndex, line.length - spaceIndex));
    }

    private static int indexOfColon(byte[] line) {
        for (int i = 0; i < line.length; i++) {
            if (line[i] == ':') {
                return i;
            }
        }
        return -1;
    }

    private void parseChunkSize(byte[] token) {
        try {
            chunkSize = Integer.parseInt(new String(token), RADIX_HEX);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("invalid chunk size");
        }
        state = chunkSize == 0
                ? State.CHUNK_TRAILER
                : State.CHUNK_DATA;
    }

    private void parseChunkData(byte[] token) {
        chunks.add(token);
        state = State.CHUNK_DATA_END;
    }

    private void parseChunkDateEnd() {
        state = State.CHUNK_SIZE;
    }

    private void parseChunkTrailer() {
        body = chunks.merge();
        state = State.DONE;
    }

    private void parseBody(byte[] token) {
        body = token;
        state = State.DONE;
    }

    private OptionalInt findContentLength() {
        try {
            return headers.stream()
                    .filter(h -> h.name().equalsIgnoreCase(HEADER_CONTENT_LENGTH))
                    .map(Header::value)
                    .mapToInt(Integer::parseInt)
                    .findFirst();
        } catch (NumberFormatException e) {
            throw new IllegalStateException("invalid content-length header value");
        }
    }

    private boolean hasChunkedEncodingHeader() {
        return headers.stream()
                .filter(h -> h.name().equalsIgnoreCase(HEADER_TRANSFER_ENCODING))
                .map(Header::value)
                .anyMatch(v -> v.equalsIgnoreCase(CHUNKED));
    }

}
