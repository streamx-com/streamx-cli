package com.streamx.cli.commands.publish.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.streamx.cli.commands.publish.event.EventTemplateCatalog.TemplateLocation;
import com.streamx.cli.config.StreamxHome;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EventTemplateCatalogTest {

  @TempDir
  Path home;

  private static final String SAMPLE_DEFAULT = """
      {"specversion":"1.0","id":"${uuid}","source":"streamx-cli",
       "type":"com.streamx.blueprints.page.published.v1",
       "datacontenttype":"application/json","subject":"${subject}",
       "time":"${currentTime}","data":{}}
      """;

  private static final String SAMPLE_USER = """
      {"specversion":"1.0","id":"${uuid}","source":"test",
       "type":"com.example.user.page.v1",
       "datacontenttype":"application/json","subject":"${subject}",
       "time":"${currentTime}","data":{}}
      """;

  private static final String SAMPLE_REGISTERED = """
      {"specversion":"1.0","id":"${uuid}","source":"test",
       "type":"com.example.registered.page.v1",
       "datacontenttype":"application/json","subject":"${subject}",
       "time":"${currentTime}","data":{}}
      """;

  @BeforeEach
  void redirectStreamxHome() {
    StreamxHome.setStreamxHomeCliArg(home.toString());
    StreamxHome.clearProfileCliArg();
  }

  @AfterEach
  void clearStreamxHome() {
    StreamxHome.clearStreamxHomeCliArg();
  }

  @Test
  void emptyHomeReturnsEmptyList() {
    assertThat(EventTemplateCatalog.listAll()).isEmpty();
    assertThat(EventTemplateCatalog.findById("page.published")).isEmpty();
  }

  @Test
  void blankIdReturnsEmptyOptional() {
    assertThat(EventTemplateCatalog.findById(null)).isEmpty();
    assertThat(EventTemplateCatalog.findById("")).isEmpty();
    assertThat(EventTemplateCatalog.findById(" ")).isEmpty();
  }

  @Test
  void findsDefaultTemplateInDefaultsFolder() throws Exception {
    seedDefault("page.published", SAMPLE_DEFAULT);

    Optional<TemplateLocation> found = EventTemplateCatalog.findById("page.published");
    assertThat(found).isPresent();
    assertThat(found.get().source()).isEqualTo(EventTemplateCatalog.SOURCE_DEFAULT);
    assertThat(found.get().type()).isEqualTo("com.streamx.blueprints.page.published.v1");
  }

  @Test
  void findsUserTemplateInUserFolder() throws Exception {
    seedUser("my.custom", SAMPLE_USER);

    Optional<TemplateLocation> found = EventTemplateCatalog.findById("my.custom");
    assertThat(found).isPresent();
    assertThat(found.get().source()).isEqualTo(EventTemplateCatalog.SOURCE_CUSTOM);
    assertThat(found.get().type()).isEqualTo("com.example.user.page.v1");
  }

  @Test
  void userFolderOverridesDefaultsFolder() throws Exception {
    seedDefault("page.published", SAMPLE_DEFAULT);
    seedUser("page.published", SAMPLE_USER);

    Optional<TemplateLocation> found = EventTemplateCatalog.findById("page.published");
    assertThat(found).isPresent();
    assertThat(found.get().source()).isEqualTo(EventTemplateCatalog.SOURCE_CUSTOM);
    assertThat(found.get().type()).isEqualTo("com.example.user.page.v1");
  }

  @Test
  void settingsRegistrationOverridesUserAndDefaults() throws Exception {
    seedDefault("page.published", SAMPLE_DEFAULT);
    seedUser("page.published", SAMPLE_USER);

    Path registered = home.resolve("profiles/default/registered.json");
    Files.writeString(registered, SAMPLE_REGISTERED);
    seedSettings("page.published", "registered.json");

    Optional<TemplateLocation> found = EventTemplateCatalog.findById("page.published");
    assertThat(found).isPresent();
    assertThat(found.get().source()).isEqualTo(EventTemplateCatalog.SOURCE_SETTINGS);
    assertThat(found.get().type()).isEqualTo("com.example.registered.page.v1");
    assertThat(found.get().path()).isEqualTo(registered.toAbsolutePath().toString());
  }

  @Test
  void listAllSortsByIdAndDeduplicates() throws Exception {
    seedDefault("zebra", SAMPLE_DEFAULT);
    seedDefault("apple", SAMPLE_DEFAULT);
    seedUser("middle", SAMPLE_USER);

    List<TemplateLocation> all = EventTemplateCatalog.listAll();
    assertThat(all).extracting(TemplateLocation::id)
        .containsExactly("apple", "middle", "zebra");
  }

  @Test
  void listSettingsRegistrationsSkipsBlankAndNonPrefixedKeys() throws Exception {
    Path some = home.resolve("profiles/default/some.json");
    Files.createDirectories(some.getParent());
    Files.writeString(some, SAMPLE_REGISTERED);
    Properties props = new Properties();
    props.setProperty("eventtemplate.real", "some.json");
    props.setProperty("eventtemplate.empty", "");
    props.setProperty("unrelated.key", "value");
    writeConfig(props);

    var registrations = EventTemplateCatalog.listSettingsRegistrations();
    assertThat(registrations).containsOnlyKeys("real");
    assertThat(registrations.get("real")).isEqualTo(some.toAbsolutePath().toString());
  }

  @Test
  void resolveRelativeToProfileDirAbsolutizesAgainstProfileDir() {
    Path resolved = EventTemplateCatalog.resolveRelativeToProfileDir("nested/file.json");
    assertThat(resolved).isAbsolute();
    assertThat(resolved)
        .isEqualTo(home.resolve("profiles/default/nested/file.json").toAbsolutePath());
  }

  @Test
  void resolveRelativeToProfileDirKeepsAbsolutePathsUntouched() {
    Path absolute = home.resolve("abs.json").toAbsolutePath();
    Path resolved = EventTemplateCatalog.resolveRelativeToProfileDir(absolute.toString());
    assertThat(resolved).isEqualTo(absolute);
  }

  @Test
  void templateIdsMirrorsListAllInOrder() throws Exception {
    seedDefault("alpha", SAMPLE_DEFAULT);
    seedDefault("beta", SAMPLE_DEFAULT);

    assertThat(EventTemplateCatalog.templateIds()).containsExactly("alpha", "beta");
  }

  private void seedDefault(String id, String body) throws Exception {
    Path dir = home.resolve(DefaultEventTemplates.DIRECTORY);
    Files.createDirectories(dir);
    Files.writeString(dir.resolve(id + DefaultEventTemplates.EXTENSION), body);
  }

  private void seedUser(String id, String body) throws Exception {
    Path dir = home.resolve("profiles/default/event-templates");
    Files.createDirectories(dir);
    Files.writeString(dir.resolve(id + UserEventTemplates.EXTENSION), body);
  }

  private void seedSettings(String id, String pathValue) throws Exception {
    Properties props = new Properties();
    props.setProperty(EventTemplateLoader.TEMPLATE_SETTINGS_MAPPING_PREFIX + id, pathValue);
    writeConfig(props);
  }

  private void writeConfig(Properties props) throws Exception {
    Path config = home.resolve("profiles/default/config/application.properties");
    Files.createDirectories(config.getParent());
    try (OutputStream out = Files.newOutputStream(config)) {
      props.store(out, null);
    }
  }
}
