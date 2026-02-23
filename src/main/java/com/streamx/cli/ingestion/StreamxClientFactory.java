package com.streamx.cli.ingestion;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.CliException;
import com.streamx.clients.ingestion.StreamxClient;
import com.streamx.clients.ingestion.StreamxClientBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class StreamxClientFactory {
  public static StreamxClient create(
      IngestionClientConfig ingestionClientConfig
  ) throws CliException {
    try {
      CloseableHttpClient httpClient = HttpClients.createDefault();

      StreamxClientBuilder builder = StreamxClient.builder(ingestionClientConfig.url())
          .setApacheHttpClient(httpClient);

      ingestionClientConfig.authToken().ifPresent(token -> builder.setAuthToken(token.get()));

      return builder.build();

      // Catch Throwable instead of Exception here because otherwise
      // we won't catch errors from HttpClients.createDefault()
    } catch (Throwable e) {
      throw new CliException(msg.unableToCreateStreamxClient(e.getMessage()), e);
    }
  }
}