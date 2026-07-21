package com.streamx.cli.auth;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.auth.OidcClient.Endpoints;
import com.streamx.cli.auth.OidcDeviceFlow.DeviceAuthorization;
import com.streamx.cli.framework.CliException;

public final class InteractiveGrant {

  private InteractiveGrant() {
  }

  public static OidcClient clientFromConfig(AuthConfig config) {
    String serverUrl = config.serverUrl()
        .filter(url -> !url.isBlank())
        .orElseThrow(() -> new CliException(
            msg.authServerUrlNotConfigured(AuthConfig.STREAMX_AUTH_SERVER_URL)));

    return new OidcClient(
        OidcClient.issuerUrl(serverUrl, config.realm()),
        config.clientId(),
        config.insecure());
  }

  public static boolean preferDeviceFlow(boolean noBrowser) {
    return noBrowser
        || System.getenv("SSH_CONNECTION") != null
        || System.getenv("SSH_TTY") != null;
  }

  public static Credentials run(OidcClient client, Endpoints endpoints, String scope,
      boolean noBrowser) {
    if (preferDeviceFlow(noBrowser)) {
      return deviceGrant(client, endpoints, scope);
    }
    try {
      return new OidcAuthCodeFlow(client).login(endpoints, scope);
    } catch (OidcAuthCodeFlow.BrowserUnavailableException e) {
      System.err.println(msg.authBrowserFallbackToDevice());
      return deviceGrant(client, endpoints, scope);
    }
  }

  private static Credentials deviceGrant(OidcClient client, Endpoints endpoints, String scope) {
    OidcDeviceFlow flow = new OidcDeviceFlow(client);
    DeviceAuthorization authorization = flow.requestDeviceAuthorization(endpoints, scope);

    System.err.println(msg.authLoginInstructions(
        authorization.verificationUri(),
        authorization.userCode()));
    if (authorization.verificationUriComplete() != null) {
      System.err.println(msg.authLoginDirectLink(authorization.verificationUriComplete()));
    }

    return flow.pollForToken(endpoints, authorization);
  }
}
