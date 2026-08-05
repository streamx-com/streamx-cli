package com.streamx.cli.platform;

import com.streamx.cli.platform.generated.api.PersonalAccessTokenResourceApi;
import com.streamx.cli.platform.generated.model.CreatePersonalAccessTokenRequest;
import com.streamx.cli.platform.generated.model.PersonalAccessTokenResponse;
import com.streamx.cli.platform.generated.model.PersonalAccessTokenSummary;
import java.time.Duration;
import java.util.List;

public class ProfileTokensApi {

  private final PlatformClients clients;
  private final PersonalAccessTokenResourceApi api;

  public ProfileTokensApi(PlatformClients clients) {
    this.clients = clients;
    this.api = clients.api(PersonalAccessTokenResourceApi.class);
  }

  public PersonalAccessTokenResponse create(String name, Duration expiresIn) {
    String lifetime = expiresIn == null ? null : expiresIn.toString();
    return clients.call(() -> api.create(
        new CreatePersonalAccessTokenRequest().name(name).expiresIn(lifetime),
        null, null), PersonalAccessTokenResponse.class);
  }

  public List<PersonalAccessTokenSummary> list() {
    return clients.callList(() -> api.callList(null, null), PersonalAccessTokenSummary.class);
  }

  public void revoke(String id) {
    clients.call(() -> api.delete(id, null, null));
  }
}
