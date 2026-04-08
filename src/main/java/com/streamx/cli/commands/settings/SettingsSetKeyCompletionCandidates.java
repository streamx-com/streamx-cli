package com.streamx.cli.commands.settings;

import com.streamx.cli.commands.publish.event.EventTemplateCatalog;
import com.streamx.cli.commands.publish.event.EventTemplateLoader;
import com.streamx.cli.ingestion.IngestionClientConfig;
import com.streamx.runner.config.StreamxBaseConfig;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

public class SettingsSetKeyCompletionCandidates implements Iterable<String> {

  static final List<String> WELL_KNOWN_KEYS = List.of(
      IngestionClientConfig.STREAMX_INGESTION_URL,
      IngestionClientConfig.STREAMX_INGESTION_AUTH_TOKEN,
      IngestionClientConfig.STREAMX_INGESTION_INSECURE,
      StreamxBaseConfig.PN_OBSERVABILITY_ENABLED,
      StreamxBaseConfig.PN_OBSERVABILITY_WAIT_FOR_STARTUP,
      StreamxBaseConfig.PN_CONTAINER_STARTUP_TIMEOUT_SECONDS,
      StreamxBaseConfig.PN_GATEWAY_BASE_HOSTNAME,
      StreamxBaseConfig.PN_TEST_DATA_LOADER_HOST,
      StreamxBaseConfig.PN_MESH_NAME_PREFIX,
      StreamxBaseConfig.PN_PULSAR_BROKER_PORT,
      StreamxBaseConfig.PN_PULSAR_HTTP_PORT,
      StreamxBaseConfig.PN_OTEL_PROMETHEUS_PORT,
      StreamxBaseConfig.PN_OTEL_GRAFANA_PORT,
      StreamxBaseConfig.PN_OTEL_TEMPO_PORT,
      StreamxBaseConfig.PN_OTEL_OTLP_GRPC_PORT,
      StreamxBaseConfig.PN_GATEWAY_HTTP_PORT,
      StreamxBaseConfig.PN_GATEWAY_ADMIN_PORT,
      StreamxBaseConfig.PN_RYUK_IMAGE,
      StreamxBaseConfig.PN_RYUK_PORT
  );

  @Override
  public Iterator<String> iterator() {
    return loadKeys().iterator();
  }

  public static Iterable<String> loadKeys() {
    TreeSet<String> keys = new TreeSet<>(WELL_KNOWN_KEYS);
    try {
      for (String templateId : EventTemplateCatalog.templateIds()) {
        keys.add(EventTemplateLoader.TEMPLATE_SETTINGS_MAPPING_PREFIX + templateId);
      }
    } catch (Exception ignored) {
      // catalog read may fail; skip eventtemplate.* completions in that case
    }
    SettingsKeyCompletionCandidates.loadKeys().forEach(keys::add);
    return keys;
  }
}
