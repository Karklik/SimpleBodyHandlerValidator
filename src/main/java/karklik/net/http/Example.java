package karklik.net.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Optional;

public class Example {
    public static void main(String[] args) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://github.com"))
                .header("Content-Type", "text/html")
                .GET()
                .build();
        HttpResponse response = null;
        try {
            response = client.send(request, responseInfo ->
                    SimpleResponseInfoValidator.newBuilder(HttpResponse.BodyHandlers.discarding())
                            .allowedStatusCode(200)
                            .requiredHeader("Content-Type",
                                    Collections.singletonList("text/html; charset=utf-8"))
                            .expectedVersion(HttpClient.Version.HTTP_2)
                            .build()
                            .apply(responseInfo));
        } catch (IOException e) {
            Class causeClass = e.getCause().getClass();
            if (causeClass.equals(HttpResponseInfoHeaderException.class)
                    || causeClass.equals(HttpResponseInfoStatusCodeException.class)
                    || causeClass.equals(HttpResponseInfoVersionException.class))
                System.out.println("I can do special logic for wrong response info!");
            else
                throw e;
        }
        Optional.ofNullable(response)
                .ifPresent(System.out::println);
    }
}
