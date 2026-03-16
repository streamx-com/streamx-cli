package com.streamx.cli.commands.publish.stream;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.CliException;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class SourceStream {

  public static InputStream get(String source) throws CliException {
    InputStream input;
    if (source != null) {
      try {
        URI uri = URI.create(source);
        if (uri.getScheme() == null) {
          input = Files.newInputStream(Path.of(source));
        } else if (uri.getScheme().matches("https?")) {
          input = openHttpStream(uri);
        } else {
          input = uri.toURL().openStream();
        }
      } catch (Exception e) {
        throw new CliException(msg.unableToOpenSourceInputStream(source, e.getMessage()), e);
      }
    } else if (System.console() != null) {
      System.err.println(msg.pasteJsonContent());
      input = System.in;
    } else {
      input = System.in;
    }

    try {
      int firstByte = input.read();
      if (firstByte == -1) {
        throw new CliException(msg.inputIsEmpty());
      }

      return new SequenceInputStream(
          new ByteArrayInputStream(new byte[]{(byte) firstByte}),
          input
      );
    } catch (Exception e) {
      throw new CliException(msg.unableToReadInputStream(e.getMessage()), e);
    }
  }

  private static InputStream openHttpStream(URI uri) throws CliException {
    HttpClient httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    try {
      HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
      HttpResponse<InputStream> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

      int status = response.statusCode();

      if (status < 200 || status >= 300) {
        String statusText = Response.Status.fromStatusCode(status).getReasonPhrase();
        httpClient.close();
        throw new CliException("HTTP " + status + " " + statusText);
      }

      return new FilterInputStream(response.body()) {
        @Override
        public void close() throws IOException {
          try {
            super.close();
          } finally {
            httpClient.close();
          }
        }
      };
    } catch (Exception e) {
      httpClient.close();
      throw new CliException(e.getMessage() == null ? msg.connectionRefused() : e.getMessage(), e);
    }
  }
}