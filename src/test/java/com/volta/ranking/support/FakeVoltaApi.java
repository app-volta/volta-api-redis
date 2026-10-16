package com.volta.ranking.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FakeVoltaApi implements AutoCloseable {

    private final HttpServer server;
    private final Map<String, Response> responses = new ConcurrentHashMap<>();
    private final Map<String, String> receivedAuthorizations = new ConcurrentHashMap<>();

    public FakeVoltaApi() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start the fake Volta API", exception);
        }

        server.createContext("/", this::handle);
        server.start();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void respond(String path, int status, String body) {
        responses.put(path, new Response(status, body));
    }

    public String receivedAuthorization(String path) {
        return receivedAuthorizations.get(path);
    }

    public void reset() {
        responses.clear();
        receivedAuthorizations.clear();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");

        if (authorization != null) {
            receivedAuthorizations.put(path, authorization);
        }

        Response response = responses.getOrDefault(path, new Response(404, ""));
        byte[] body = response.body().getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(response.status(), body.length == 0 ? -1 : body.length);

        if (body.length > 0) {
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        }

        exchange.close();
    }

    private record Response(int status, String body) {
    }
}
