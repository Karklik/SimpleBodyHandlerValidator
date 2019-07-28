package karklik.net.http;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Builder(builderMethodName = "newBuilder")
public class SimpleBodyHandlerValidator<T> implements HttpResponse.BodyHandler<T> {
    @NonNull
    private final HttpResponse.BodyHandler<T> handler;
    @Singular
    private Set<Integer> allowedStatusCodes;
    @Singular
    private Map<String, List<String>> requiredHeaders;
    private HttpClient.Version expectedVersion;

    public static <T> SimpleBodyHandlerValidatorBuilder<T> newBuilder(HttpResponse.BodyHandler<T> handler) {
        SimpleBodyHandlerValidatorBuilder<T> builder = new SimpleBodyHandlerValidatorBuilder<T>();
        builder.handler(handler);
        return builder;
    }

    @Override
    public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
        Optional.of(allowedStatusCodes)
                .filter(integers -> !integers.isEmpty())
                .ifPresent(integers -> integers.stream()
                        .filter(value -> value == responseInfo.statusCode())
                        .findAny()
                        .orElseThrow(() ->
                                new HttpResponseInfoStatusCodeException("Received status code: "
                                        + responseInfo.statusCode()
                                        + ", Allowed status codes: "
                                        + allowedStatusCodes)));
        // TODO: Add checking for exact requiredHeader values instead of comparing full header
        Optional.of(requiredHeaders)
                .filter(map -> !map.isEmpty())
                .ifPresent(map -> map.entrySet()
                        .parallelStream()
                        .forEach(requiredHeader -> responseInfo.headers()
                                .map()
                                .entrySet()
                                .parallelStream()
                                .filter(header -> requiredHeader.getKey().equalsIgnoreCase(header.getKey())
                                        && requiredHeader.getValue().equals(header.getValue()))
                                .findAny()
                                .orElseThrow(() ->
                                        new HttpResponseInfoHeaderException("Missing requiredHeader: "
                                                + requiredHeader
                                                + ", Received headers: "
                                                + responseInfo.headers()
                                                + ", Expected headers: "
                                                + requiredHeaders))));
        Optional.ofNullable(expectedVersion)
                .filter(v -> responseInfo.version().equals(v))
                .orElseThrow(() -> new HttpResponseInfoVersionException("Received response version: "
                        + responseInfo.version()
                        + ", Expected response version: "
                        + expectedVersion));
        return handler.apply(responseInfo);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofMillis(30000))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://github.com"))
                .header("Content-Type", "text/html")
                .GET()
                .build();
        HttpResponse response = client.send(request, responseInfo ->
                SimpleBodyHandlerValidator.newBuilder(HttpResponse.BodyHandlers.discarding())
                        .allowedStatusCode(200)
                        .requiredHeader("Content-Type",
                                Collections.singletonList("text/html; charset=utf-8"))
                        .build()
                        .apply(responseInfo));
    }
}
