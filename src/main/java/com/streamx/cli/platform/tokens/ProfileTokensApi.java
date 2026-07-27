package com.streamx.cli.platform.tokens;

import com.streamx.cli.platform.PlatformClients;
import java.util.List;

public class ProfileTokensApi {

  private final PlatformClients clients;
  private final ProfileTokensClient api;

  public ProfileTokensApi(PlatformClients clients) {
    this.clients = clients;
    this.api = clients.api(ProfileTokensClient.class);
  }

  public TokenResponse create(String name) {
    return clients.call(() -> api.create(new CreateTokenRequest(name)), TokenResponse.class);
  }

  public List<TokenSummary> list() {
    return clients.callList(() -> api.list(), TokenSummary.class);
  }

  public void revoke(String id) {
    clients.call(() -> api.delete(id));
  }
}
