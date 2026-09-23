package com.streamx.cli.platform;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;

public class AuthHeaderFilter implements ClientRequestFilter {

  @Override
  public void filter(ClientRequestContext context) {
    context.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, "Bearer " + AccessTokens.current());
    context.getHeaders().putSingle(HttpHeaders.ACCEPT, "application/json");
  }
}
