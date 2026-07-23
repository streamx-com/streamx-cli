package com.streamx.cli.platform;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class OrganizationInvitationsApi {

  private final PlatformApiClient client;

  public OrganizationInvitationsApi(PlatformApiClient client) {
    this.client = client;
  }

  public List<Invitation> list(String orgId) {
    List<Invitation> invitations = new ArrayList<>();
    for (JsonNode node : client.get(invitationsPath(orgId))) {
      invitations.add(Invitation.fromJson(node));
    }
    invitations.sort(
        Comparator.comparing(Invitation::email, Comparator.nullsLast(String::compareTo)));
    return invitations;
  }

  public void create(String orgId, String email, String role) {
    client.postJson(invitationsPath(orgId), Map.of("email", email, "role", role));
  }

  public void accept(String orgId, String token) {
    client.patchJson(invitationsPath(orgId), Map.of("token", token));
  }

  public void cancel(String orgId, String email) {
    String emailBase64 = Base64.getEncoder()
        .encodeToString(email.getBytes(StandardCharsets.UTF_8));
    client.delete(invitationsPath(orgId) + "/" + PathSegments.encode(emailBase64));
  }

  private static String invitationsPath(String orgId) {
    return "/api/v1/organizations/" + PathSegments.encode(orgId) + "/invitations";
  }
}
