package com.streamx.cli.commands.org;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.platform.PlatformConfig;
import com.streamx.cli.test.CliBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OrgInvitationsCommandIT extends CliBaseIT {

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
  void shouldListInvitations() throws Exception {
    ProcessResult result = exec("org", "invitations", "list", ORG);

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("GET /api/v1/organizations/" + ORG + "/invitations");
    assertThat(result.stdout()).contains("EMAIL", "ROLE", "STATUS");
    assertThat(result.stdout()).contains("invited@streamx.com", "edit", "PENDING");
  }

  @Test
  void shouldCreateInvitation() throws Exception {
    ProcessResult result =
        exec("org", "invitations", "create", ORG, "new@streamx.com", "--role", "view");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("POST /api/v1/organizations/" + ORG + "/invitations");
    assertThat(platform.getRequestBodies().get(0))
        .contains("\"email\":\"new@streamx.com\"", "\"role\":\"view\"");
    assertThat(result.stdout()).contains(msg.orgInvitationCreated("new@streamx.com", "view"));
  }

  /** The token is a credential, so it is read from stdin rather than taken from argv. */
  @Test
  void shouldAcceptInvitationWithTokenFromStdin() throws Exception {
    ProcessResult result =
        execWithStdin("token-abc\n", "org", "invitations", "accept", ORG);

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("PATCH /api/v1/organizations/" + ORG + "/invitations");
    assertThat(platform.getRequestBodies().get(0)).contains("\"token\":\"token-abc\"");
    assertThat(result.stdout()).contains(msg.orgInvitationAccepted());
  }

  @Test
  void shouldAcceptInvitationWithTokenFromFile() throws Exception {
    Path tokenFile = streamxHome.resolve("token.txt");
    Files.writeString(tokenFile, "token-from-file\n");

    ProcessResult result = exec(
        "org", "invitations", "accept", ORG, "--token-file", tokenFile.toString());

    result.assertSuccess();
    assertThat(platform.getRequestBodies().get(0)).contains("\"token\":\"token-from-file\"");
  }

  @Test
  void shouldFailWhenNoInvitationTokenSupplied() throws Exception {
    ProcessResult result = execWithStdin("", "org", "invitations", "accept", ORG);

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.orgInvitationTokenRequired());
    assertThat(platform.getRequests()).isEmpty();
  }

  /** The server takes the email base64-encoded in the path; the CLI must hide that. */
  @Test
  void shouldCancelInvitationSendingBase64EncodedEmailInPath() throws Exception {
    String email = "invited@streamx.com";
    String expected = Base64.getEncoder().encodeToString(email.getBytes(StandardCharsets.UTF_8));

    ProcessResult result = exec("org", "invitations", "cancel", ORG, email);

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("DELETE /api/v1/organizations/" + ORG + "/invitations/" + expected);
    assertThat(result.stdout()).contains(msg.orgInvitationCancelled(email));
  }
}
