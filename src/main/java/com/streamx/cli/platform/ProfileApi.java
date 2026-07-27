package com.streamx.cli.platform;

import com.streamx.cli.platform.generated.api.ProfileResourceApi;
import com.streamx.cli.platform.generated.model.Profile;

public class ProfileApi {

  private final PlatformClients clients;
  private final ProfileResourceApi api;

  public ProfileApi(PlatformClients clients) {
    this.clients = clients;
    this.api = clients.api(ProfileResourceApi.class);
  }

  public Profile get() {
    return clients.call(() -> api.getProfile(null, null), Profile.class);
  }
}
