package com.streamx.cli.ingestion;

import com.streamx.cli.framework.CliException;
import com.streamx.clients.ingestion.StreamxClient;
import com.streamx.clients.ingestion.StreamxClientBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class StreamxClientFactory {
  public StreamxClient create(IngestionClientConfig ingestionClientConfig) throws CliException {
    CloseableHttpClient httpClient = HttpClients.createDefault();

    StreamxClientBuilder builder = StreamxClient.builder(ingestionClientConfig.url())
        .setApacheHttpClient(httpClient);

    ingestionClientConfig.authToken().ifPresent(builder::setAuthToken);

    try {
      return builder.build();
    } catch (Exception e) {
      throw new CliException("Unable to create StreamX client. %s".formatted(e.getMessage()), e);
    }
  }
}
