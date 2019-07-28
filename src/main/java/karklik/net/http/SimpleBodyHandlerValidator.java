package karklik.net.http;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
                .filter(codes -> !codes.isEmpty())
                .ifPresent(integers -> checkStatusCode(responseInfo));
        // TODO: Add checking for exact requiredHeader values instead of comparing full header
        Optional.of(requiredHeaders)
                .filter(headers -> !headers.isEmpty())
                .ifPresent(headers -> checkHeaders(responseInfo));
        if (expectedVersion != null)
            checkVersion(responseInfo);
        return handler.apply(responseInfo);
    }


    private void checkHeaders(HttpResponse.ResponseInfo responseInfo) {
        requiredHeaders.entrySet()
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
                                        + requiredHeaders)));
    }

    private void checkStatusCode(HttpResponse.ResponseInfo responseInfo) {
        allowedStatusCodes.stream()
                .filter(value -> value == responseInfo.statusCode())
                .findAny()
                .orElseThrow(() ->
                        new HttpResponseInfoStatusCodeException("Received status code: "
                                + responseInfo.statusCode()
                                + ", Allowed status codes: "
                                + allowedStatusCodes));
    }

    private void checkVersion(HttpResponse.ResponseInfo responseInfo) {
        Optional.of(expectedVersion)
                .filter(version -> responseInfo.version().equals(version))
                .orElseThrow(() -> new HttpResponseInfoVersionException("Received response version: "
                        + responseInfo.version()
                        + ", Expected response version: "
                        + expectedVersion));
    }
}
