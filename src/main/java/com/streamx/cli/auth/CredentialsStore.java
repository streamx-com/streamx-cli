package com.streamx.cli.auth;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.streamx.cli.config.StreamxHome;
import com.streamx.cli.framework.CliException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public class CredentialsStore {
  private static final String CREDENTIALS_FILE = "config/credentials.json";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String ACCESS_TOKEN = "access_token";
  private static final String REFRESH_TOKEN = "refresh_token";
  private static final String EXPIRES_AT = "expires_at";
  private static final String ISSUER_URL = "issuer_url";
  private static final String CLIENT_ID = "client_id";
  private static final String INSECURE = "insecure";

  public static Path getCredentialsPath() {
    return StreamxHome.getStreamxHome().resolve(CREDENTIALS_FILE);
  }

  public static boolean exists() {
    return Files.isRegularFile(getCredentialsPath());
  }

  public static Optional<Credentials> load() {
    Path path = getCredentialsPath();
    if (!Files.isRegularFile(path)) {
      return Optional.empty();
    }

    try {
      JsonNode node = MAPPER.readTree(Files.readString(path));
      return Optional.of(new Credentials(
          node.path(ACCESS_TOKEN).asText(null),
          node.path(REFRESH_TOKEN).asText(null),
          Instant.ofEpochSecond(node.path(EXPIRES_AT).asLong()),
          node.path(ISSUER_URL).asText(null),
          node.path(CLIENT_ID).asText(null),
          node.path(INSECURE).asBoolean(false)
      ));
    } catch (IOException e) {
      throw new CliException(msg.authCredentialsUnreadable(path.toString(), e.getMessage()), e);
    }
  }

  public static void save(Credentials credentials) {
    Path path = getCredentialsPath();

    ObjectNode node = MAPPER.createObjectNode();
    node.put(ACCESS_TOKEN, credentials.accessToken());
    node.put(REFRESH_TOKEN, credentials.refreshToken());
    node.put(EXPIRES_AT, credentials.expiresAt().getEpochSecond());
    node.put(ISSUER_URL, credentials.issuerUrl());
    node.put(CLIENT_ID, credentials.clientId());
    node.put(INSECURE, credentials.insecure());

    Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
    try {
      Files.createDirectories(path.getParent());
      Files.deleteIfExists(temporary);
      createOwnerOnlyFile(temporary);
      Files.writeString(temporary, MAPPER.writeValueAsString(node));
      moveIntoPlace(temporary, path);
    } catch (IOException e) {
      quietlyDelete(temporary);
      throw new CliException(msg.authCredentialsNotSaved(path.toString(), e.getMessage()), e);
    }
  }

  private static void moveIntoPlace(Path temporary, Path path) throws IOException {
    try {
      Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static void quietlyDelete(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException expected) {
    }
  }

  public static void delete() {
    Path path = getCredentialsPath();
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      throw new CliException(msg.authCredentialsNotDeleted(path.toString(), e.getMessage()), e);
    }
  }

  private static void createOwnerOnlyFile(Path path) throws IOException {
    if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
      Files.createFile(path, PosixFilePermissions.asFileAttribute(
          Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)));
    } else {
      Files.createFile(path);
    }
  }
}
