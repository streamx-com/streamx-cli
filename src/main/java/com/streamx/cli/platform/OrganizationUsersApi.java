package com.streamx.cli.platform;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.CliException;
import com.streamx.cli.platform.generated.api.UsersResourceApi;
import com.streamx.cli.platform.generated.model.NameAndRole;
import com.streamx.cli.platform.generated.model.RoleChange;
import com.streamx.cli.platform.generated.model.User;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

public class OrganizationUsersApi {

  private final PlatformClients clients;
  private final UsersResourceApi api;

  public OrganizationUsersApi(PlatformClients clients) {
    this.clients = clients;
    this.api = clients.api(UsersResourceApi.class);
  }

  public List<User> list(String orgId) {
    return clients.callList(() -> api.listUsers(orgId, null, null), User.class).stream()
        .sorted(Comparator.comparing(User::getId, Comparator.nullsLast(String::compareTo)))
        .toList();
  }

  public User requireActiveMember(String orgId, String userId,
      BiFunction<String, String, String> notActive) {
    User member = find(orgId, userId)
        .orElseThrow(() -> new CliException(msg.orgMemberNotFound(userId, orgId)));
    if (member.getStatus() != User.StatusEnum.ACTIVE) {
      String status = member.getStatus() == null ? "" : member.getStatus().value();
      throw new CliException(notActive.apply(userId, status));
    }
    return member;
  }

  public Optional<User> find(String orgId, String userId) {
    return clients.callList(() -> api.listUsers(orgId, null, null), User.class).stream()
        .filter(user -> Objects.equals(user.getId(), userId))
        .findFirst();
  }

  public void add(String orgId, String email, String role) {
    clients.call(() -> api.addUserToOrganization(
        orgId, new NameAndRole().name(email).role(role), null, null));
  }

  public void remove(String orgId, String userId) {
    clients.call(() -> api.removeUserFromOrganization(orgId, userId, null, null));
  }

  public void editRole(String orgId, String userId, String newRoleId) {
    clients.call(() -> api.editUserRoles(
        orgId, userId, new RoleChange().newRoleId(newRoleId), null, null));
  }
}
