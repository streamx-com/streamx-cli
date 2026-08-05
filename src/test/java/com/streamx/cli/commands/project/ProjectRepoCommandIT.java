package com.streamx.cli.commands.project;

import static com.streamx.cli.i18n.MessageProvider.msg;
import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.platform.PlatformConfig;
import com.streamx.cli.test.CliBaseIT;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Base64;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProjectRepoCommandIT extends CliBaseIT {

  private static final String ORG = "so-testorg";
  private static final String PROJECT = "so-org-web-a1b2c";
  private static final String REPO_PATH =
      "/api/v1/organizations/" + ORG + "/projects/" + PROJECT + "/repository";

  private StubProjectServer platform;

  private Path getCredentialsPath() {
    return streamxHome.resolve("contexts/default/config/credentials.json");
  }

  @BeforeEach
  void setUp() throws IOException {
    platform = new StubProjectServer();

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
    Files.deleteIfExists(streamxHome.resolve("contexts/default/current-org"));
    Files.deleteIfExists(streamxHome.resolve("contexts/default/current-project"));
  }

  private ProcessResult repo(String... args) throws Exception {
    String[] base = {"project", "repo"};
    String[] full = new String[base.length + args.length + 4];
    System.arraycopy(base, 0, full, 0, base.length);
    System.arraycopy(args, 0, full, base.length, args.length);
    full[base.length + args.length] = "--org";
    full[base.length + args.length + 1] = ORG;
    full[base.length + args.length + 2] = "--project";
    full[base.length + args.length + 3] = PROJECT;
    return exec(full);
  }

  @Test
  void setConnectsWhenNoRepositoryYet() throws Exception {
    platform.returnNoRepository();

    ProcessResult result = repo("set", "--uri", "git@github.com:acme/web.git",
        "--branch", "main");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("GET " + REPO_PATH, "POST " + REPO_PATH);
    assertThat(platform.getRequestBodies().get(1))
        .contains("\"uri\":\"git@github.com:acme/web.git\"")
        .contains("\"branch\":\"main\"");
    assertThat(result.stdout()).contains(msg.projectRepoConnected(PROJECT));
  }

  @Test
  void setUpdatesWhenRepositoryExists() throws Exception {
    ProcessResult result = repo("set", "--uri", "git@github.com:acme/web.git",
        "--branch", "develop");

    result.assertSuccess();
    assertThat(platform.getRequests())
        .containsExactly("GET " + REPO_PATH, "PATCH " + REPO_PATH);
    assertThat(result.stdout()).contains(msg.projectRepoUpdated(PROJECT));
  }

  @Test
  void getShowsRepositoryDetails() throws Exception {
    ProcessResult result = repo("get");

    result.assertSuccess();
    assertThat(result.stdout())
        .contains("uri       = git@github.com:acme/web.git")
        .contains("branch    = main")
        .contains("commit    = abc1234")
        .contains("ready     = true");
  }

  @Test
  void getFailsCleanlyWithoutRepository() throws Exception {
    platform.returnNoRepository();

    ProcessResult result = repo("get");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains("has no repository connected");
  }

  @Test
  void removeDisconnectsRepository() throws Exception {
    ProcessResult result = repo("remove");

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly("DELETE " + REPO_PATH);
    assertThat(result.stdout()).contains(msg.projectRepoRemoved(PROJECT));
  }

  @Test
  void sshKeySetCreatesWhenAbsent() throws Exception {
    Path keyFile = streamxHome.resolve("deploy-key");
    Files.writeString(keyFile, "KEY BYTES");
    String expected = Base64.getEncoder()
        .encodeToString("KEY BYTES".getBytes(StandardCharsets.UTF_8));

    ProcessResult result = repo("ssh-key", "set", keyFile.toString());

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly(
        "GET " + REPO_PATH + "/ssh-key/exists",
        "POST " + REPO_PATH + "/ssh-key");
    assertThat(platform.getRequestBodies().get(1))
        .contains("\"privateKeyBase64\":\"" + expected + "\"");
    assertThat(result.stdout()).contains(msg.projectSshKeySet(PROJECT));
  }

  @Test
  void sshKeySetReplacesWhenPresent() throws Exception {
    platform.sshKeyExists(true);
    Path keyFile = streamxHome.resolve("deploy-key");
    Files.writeString(keyFile, "KEY BYTES");

    ProcessResult result = repo("ssh-key", "set", keyFile.toString());

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly(
        "GET " + REPO_PATH + "/ssh-key/exists",
        "PATCH " + REPO_PATH + "/ssh-key");
  }

  @Test
  void sshKeyShowPrintsPublicKey() throws Exception {
    platform.sshKeyExists(true);

    ProcessResult result = repo("ssh-key", "show");

    result.assertSuccess();
    assertThat(result.stdout()).contains("ssh-ed25519 STORED-PUBLIC");
  }

  @Test
  void sshKeyShowFailsCleanlyWithoutKey() throws Exception {
    ProcessResult result = repo("ssh-key", "show");

    result.assertExitCode(1);
    assertThat(result.stderr()).contains("has no SSH key configured");
  }

  @Test
  void sshKeyRemoveDeletesKey() throws Exception {
    platform.sshKeyExists(true);

    ProcessResult result = repo("ssh-key", "remove");

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly("DELETE " + REPO_PATH + "/ssh-key");
    assertThat(result.stdout()).contains(msg.projectSshKeyRemoved(PROJECT));
  }

  @Test
  void sshKeyGenerateWritesKeyPairFilesWithoutPrintingKeys() throws Exception {
    Path keyFile = streamxHome.resolve("generated-key");

    ProcessResult result = exec(
        "project", "repo", "ssh-key", "generate", keyFile.toString(), "--org", ORG);

    result.assertSuccess();
    assertThat(platform.getRequests()).containsExactly(
        "POST /api/v1/organizations/" + ORG + "/projects/repository/ssh-key/generate-key-pair");
    Path publicKeyFile = streamxHome.resolve("generated-key.pub");
    assertThat(keyFile).content().isEqualTo("GENERATED-PRIVATE\n");
    assertThat(publicKeyFile).content().isEqualTo("ssh-ed25519 GENERATED-PUBLIC\n");
    assertThat(Files.getPosixFilePermissions(keyFile))
        .isEqualTo(PosixFilePermissions.fromString("rw-------"));
    assertThat(result.stdout())
        .contains(msg.projectSshKeyPairWritten(keyFile.toString(), publicKeyFile.toString()))
        .doesNotContain("GENERATED-PRIVATE");
  }

  @Test
  void sshKeyGenerateRefusesToOverwriteExistingFile() throws Exception {
    Path keyFile = streamxHome.resolve("existing-key");
    Files.writeString(keyFile, "old");

    ProcessResult result = exec(
        "project", "repo", "ssh-key", "generate", keyFile.toString(), "--org", ORG);

    result.assertExitCode(1);
    assertThat(result.stderr()).contains(msg.projectSshKeyFileExists(keyFile.toString()));
    assertThat(platform.getRequests()).isEmpty();
    assertThat(keyFile).content().isEqualTo("old");
  }
}
