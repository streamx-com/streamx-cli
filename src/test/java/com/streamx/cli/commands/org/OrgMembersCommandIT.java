package com.streamx.cli.commands.org;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.platform.PlatformConfig;
import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OrgMembersCommandIT extends CliBaseIT {

  private static final String ORG = "so-testorg";

  private StubPlatformServer platform;

  private Path getCredentialsPath() {
    return streamxHome.resolve("profiles/default/config/credentials.json");
  }

  @BeforeEach
  void setUp() throws IOException {
    platform = new StubPlatformServer();

    Properties properties = new Properties();
    properties.setProperty(PlatformConfig.STREAMX_PLATFORM_URL, platform.getUrl());
    Path configFile = getConfigPath();
    Files.createDirectories(configFile.getParent());
    try (OutputStream out = Files.newOutputStream(configFile)) {
      properties.store(out, null);
    }

    Path credentials = getCredentialsPath();
    Files.createDirectories(credentials.getParent());
    Files.writeString(credentials, """
        {"access_token":"test-access-token","refresh_token":"test-refresh-token",
         "expires_at":%d,"issuer_url":"http://127.0.0.1:1/realms/streamx",
         "client_id":"streamx-cli"}
        """.formatted(Instant.now().plusSeconds(300).getEpochSecond()));
  }

  @AfterEach
  void tearDown() throws IOException {
    if (platform != null) {
      platform.close();
    }
    Files.deleteIfExists(getCredentialsPath());
  }

  @Test
  void shouldListMembersMarkingTheCaller() throws Exception {
    ProcessResult result = exec("org", "members", "list", ORG);

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("GET /api/v1/organizations/" + ORG + "/users");
    assertThat(result.stdout()).contains("user1@streamx.com", "owner", "ACTIVE", "(you)");
    assertThat(result.stdout()).contains("pending@streamx.com", "view", "PENDING");
  }

  @Test
  void completeOrgMemberIdsListsOnlyActiveMembers() throws Exception {
    ProcessResult result = exec("__complete-org-member-ids", ORG);

    result.assertSuccess();
    assertThat(result.stdout().strip().lines())
        .containsExactly("active@streamx.com", "user1@streamx.com");
  }

  @Test
  void shouldListOnlyMemberIdsWhenQuiet() throws Exception {
    ProcessResult result = exec("org", "members", "list", ORG, "-q");

    result.assertSuccess();
    assertThat(result.stdout())
        .isEqualTo("active@streamx.com\npending@streamx.com\nuser1@streamx.com\n");
  }

  @Test
  void shouldAddMemberByEmailWithRole() throws Exception {
    ProcessResult result =
        exec("org", "members", "add", ORG, "existing@streamx.com", "--role", "edit");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("POST /api/v1/organizations/" + ORG + "/users");
    assertThat(platform.getRequestBodies().get(0))
        .contains("\"name\":\"existing@streamx.com\"", "\"role\":\"edit\"");
    assertThat(result.stdout()).contains(msg.orgMemberAdded("existing@streamx.com", "edit"));
  }

  @Test
  void shouldRemoveActiveMember() throws Exception {
    ProcessResult result = exec("org", "members", "remove", ORG, "active@streamx.com");

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly(
        "GET /api/v1/organizations/" + ORG + "/users",
        "DELETE /api/v1/organizations/" + ORG + "/users/active@streamx.com");
    assertThat(result.stdout()).contains(msg.orgMemberRemoved("active@streamx.com"));
  }

  /** The server rejects removing a principal that is not an active member; say so up front. */
  @Test
  void shouldRefuseToRemovePendingInvitation() throws Exception {
    ProcessResult result = exec("org", "members", "remove", ORG, "pending@streamx.com");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains("invitations cancel");
    assertThat(platform.getRequests())
        .containsExactly("GET /api/v1/organizations/" + ORG + "/users");
  }

  @Test
  void shouldChangeRoleOfActiveMember() throws Exception {
    ProcessResult result =
        exec("org", "members", "set-role", ORG, "active@streamx.com", "--role", "owner");

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly(
        "GET /api/v1/organizations/" + ORG + "/users",
        "PUT /api/v1/organizations/" + ORG + "/users/active@streamx.com");
    assertThat(platform.getRequestBodies().get(1)).contains("\"newRoleId\":\"owner\"");
    assertThat(result.stdout()).contains(msg.orgMemberRoleChanged("active@streamx.com", "owner"));
  }

  /** Server-side the role change is remove-then-add, which would activate a pending invitation. */
  @Test
  void shouldRefuseToChangeRoleOfPendingInvitation() throws Exception {
    ProcessResult result =
        exec("org", "members", "set-role", ORG, "pending@streamx.com", "--role", "owner");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains("without the invitation being accepted");
    assertThat(platform.getRequests())
        .containsExactly("GET /api/v1/organizations/" + ORG + "/users");
  }

  @Test
  void shouldRejectUnknownMember() throws Exception {
    ProcessResult result = exec("org", "members", "remove", ORG, "nobody@streamx.com");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.orgMemberNotFound("nobody@streamx.com", ORG));
  }

  @Test
  void shouldRequireRoleWhenAddingMember() throws Exception {
    ProcessResult result = exec("org", "members", "add", ORG, "someone");

    result.assertExitCode(2);
    assertThat(platform.getRequests()).isEmpty();
  }
}
