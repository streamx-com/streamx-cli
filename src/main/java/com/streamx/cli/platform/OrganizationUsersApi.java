package com.streamx.cli.platform;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OrganizationUsersApi {

  private final PlatformApiClient client;

  public OrganizationUsersApi(PlatformApiClient client) {
    this.client = client;
  }

  public List<User> list(String orgId) {
    List<User> users = new ArrayList<>();
    for (JsonNode node : client.get(usersPath(orgId))) {
      users.add(User.fromJson(node));
    }
    users.sort(Comparator.comparing(User::id, Comparator.nullsLast(String::compareTo)));
    return users;
  }

  public Optional<User> find(String orgId, String userId) {
    return list(orgId).stream()
        .filter(user -> userId.equals(user.id()))
        .findFirst();
  }

  public void add(String orgId, String email, String role) {
    client.postJson(usersPath(orgId), Map.of("name", email, "role", role));
  }

  public void remove(String orgId, String userId) {
    client.delete(usersPath(orgId) + "/" + PathSegments.encode(userId));
  }

  public void editRole(String orgId, String userId, String newRoleId) {
    client.putJson(
        usersPath(orgId) + "/" + PathSegments.encode(userId),
        Map.of("newRoleId", newRoleId));
  }

  private static String usersPath(String orgId) {
    return "/api/v1/organizations/" + PathSegments.encode(orgId) + "/users";
  }
}
