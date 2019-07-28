# SimpleResponseInfoValidator
It is simple wrapping builder class for `java.net.http.HttpResponse.BodyHandler<T>` which allows do assertion if incoming http response have desired status code, headers or protocol version before receiving full http response.
```
A BodyHandler is a function that takes a ResponseInfo object; and which returns a BodySubscriber. The BodyHandler is invoked when the response status code and headers are available, but before the response body bytes are received. 
```
[More JDK docs](https://docs.oracle.com/en/java/javase/12/docs/api/java.net.http/java/net/http/HttpResponse.BodyHandler.html)

In case of unfulfilled assumptions `java.io.IOException` will be thrown, with one of following cause classes `HttpResponseInfoHeaderException`, `HttpResponseInfoStatusCodeException`, `HttpResponseInfoVersionException`.

## Getting Started
### Prerequisites
* Gradle
* JavaJDK 12+

### Building
`gradle build` > jar should be generated in default build directory i.e. `build/libs/SimpleResponseInfoValidator-1.0.jar`

### Usage

```java
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
                    SimpleResponseInfoValidator.newBuilder(HttpResponse.BodyHandlers.ofString())
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
```
## Versioning
[SemVer](https://semver.org/)

## License
Project is under the MIT License
