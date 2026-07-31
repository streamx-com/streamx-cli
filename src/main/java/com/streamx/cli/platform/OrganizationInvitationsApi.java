package com.streamx.cli.platform;

import com.streamx.cli.platform.generated.api.InvitationsResourceApi;
import com.streamx.cli.platform.generated.model.Invitation;
import com.streamx.cli.platform.generated.model.InvitationAccept;
import com.streamx.cli.platform.generated.model.InvitationRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

public class OrganizationInvitationsApi {

  private final PlatformClients clients;
  private final InvitationsResourceApi api;

  public OrganizationInvitationsApi(PlatformClients clients) {
    this.clients = clients;
    this.api = clients.api(InvitationsResourceApi.class);
  }

  public List<Invitation> list(String orgId) {
    return clients.callList(() -> api.listInvitations(orgId, null, null), Invitation.class).stream()
        .sorted(Comparator.comparing(Invitation::getEmail, Comparator.nullsLast(String::compareTo)))
        .toList();
  }

  public void create(String orgId, String email, String role) {
    clients.call(() -> api.createInvitation(
        orgId, new InvitationRequest().email(email).role(role), null, null));
  }

  public void accept(String orgId, String token) {
    clients.call(() -> api.acceptInvitation(
        orgId, new InvitationAccept().token(token), null, null));
  }

  public void cancel(String orgId, String email) {
    String emailBase64 = Base64.getEncoder()
        .encodeToString(email.getBytes(StandardCharsets.UTF_8));
    clients.call(() -> api.cancelInvitation(emailBase64, orgId, null, null));
  }
}
