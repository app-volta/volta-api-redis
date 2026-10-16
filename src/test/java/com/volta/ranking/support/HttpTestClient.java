package com.volta.ranking.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpTestClient {

    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final int port;

    public HttpTestClient(int port) {
        this.port = port;
    }

    public String login(String user) {
        String body = "{\"email\":\"" + user + "@volta.com\",\"password\":\"senha123\"}";
        HttpResponse<String> response = post("/api/v1/auth/login", null, body);

        assertThat(response.statusCode()).as("login as %s", user).isEqualTo(200);

        Matcher matcher = TOKEN.matcher(response.body());
        assertThat(matcher.find()).as("token in login response").isTrue();
        return matcher.group(1);
    }

    public HttpResponse<String> get(String path, String token) {
        return send(request(path, token).GET().build());
    }

    public HttpResponse<String> post(String path, String token, String json) {
        HttpRequest request = request(path, token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return send(request);
    }

    public HttpResponse<String> put(String path, String token, String json) {
        HttpRequest request = request(path, token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return send(request);
    }

    public HttpResponse<String> delete(String path, String token) {
        return send(request(path, token).DELETE().build());
    }

    private HttpRequest.Builder request(String path, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        return builder;
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new IllegalStateException("HTTP call failed: " + request.uri(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP call interrupted: " + request.uri(), exception);
        }
    }
}
