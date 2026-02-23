package com.streamx.cli.test;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.datafaker.Faker;

public class CloudEventGenerator {

  private static final String DEFAULT_SOURCE = "streamx-commerce-accelerator";
  private static final String DEFAULT_TYPE = "com.streamx.blueprints.data.published.v1";
  private static final String DEFAULT_DATA_CONTENT_TYPE = "application/json";
  private static final String DEFAULT_DATA_TYPE = "data/category";
  private static final Faker FAKER = new Faker();

  private final String source;
  private final String type;
  private final String dataContentType;
  private final Function<String, String> subjectProvider;
  private final Supplier<String> idProvider;
  private final Supplier<OffsetDateTime> timeProvider;
  private final Function<String, String> dataProvider;

  public CloudEventGenerator() {
    this.source = DEFAULT_SOURCE;
    this.type = DEFAULT_TYPE;
    this.dataContentType = DEFAULT_DATA_CONTENT_TYPE;
    this.subjectProvider = id -> "cat:" + id;
    this.idProvider = () -> FAKER.commerce().department();
    this.timeProvider = () -> OffsetDateTime.now(ZoneOffset.UTC);
    this.dataProvider = id -> defaultData(id, DEFAULT_DATA_TYPE);
  }

  private CloudEventGenerator(Builder builder) {
    this.source = builder.source;
    this.type = builder.type;
    this.dataContentType = builder.dataContentType;
    this.subjectProvider = builder.subjectProvider;
    this.idProvider = builder.idProvider;
    this.timeProvider = builder.timeProvider;
    this.dataProvider = builder.dataProvider;
  }

  public List<CloudEvent> generate(int n) {
    List<CloudEvent> events = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      String id = idProvider.get();
      String data = dataProvider.apply(id);

      CloudEvent event = CloudEventBuilder.v1()
          .withId(id)
          .withSource(URI.create(source))
          .withType(type)
          .withDataContentType(dataContentType)
          .withSubject(subjectProvider.apply(id))
          .withTime(timeProvider.get())
          .withData(dataContentType, data.getBytes(StandardCharsets.UTF_8))
          .build();

      events.add(event);
    }
    return events;
  }

  public static Builder builder() {
    return new Builder();
  }

  private static String escapeJson(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private static String defaultData(String id, String dataType) {
    return "{\"content\":\"{\\\"id\\\":\\\"" + escapeJson(id) + "\\\"}\","
        + "\"type\":\"" + dataType + "\"}";
  }

  public static class Builder {

    private String source = DEFAULT_SOURCE;
    private String type = DEFAULT_TYPE;
    private String dataContentType = DEFAULT_DATA_CONTENT_TYPE;
    private String dataType = DEFAULT_DATA_TYPE;
    private Function<String, String> subjectProvider = id -> "cat:" + id;
    private Supplier<String> idProvider = () -> FAKER.commerce().department();
    private Supplier<OffsetDateTime> timeProvider = () -> OffsetDateTime.now(ZoneOffset.UTC);
    private Function<String, String> dataProvider;

    private Builder() {
    }

    public Builder source(String source) {
      this.source = source;
      return this;
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder dataContentType(String dataContentType) {
      this.dataContentType = dataContentType;
      return this;
    }

    public Builder dataType(String dataType) {
      this.dataType = dataType;
      return this;
    }

    public Builder subjectProvider(Function<String, String> subjectProvider) {
      this.subjectProvider = subjectProvider;
      return this;
    }

    public Builder idProvider(Supplier<String> idProvider) {
      this.idProvider = idProvider;
      return this;
    }

    public Builder timeProvider(Supplier<OffsetDateTime> timeProvider) {
      this.timeProvider = timeProvider;
      return this;
    }

    public Builder dataProvider(Function<String, String> dataProvider) {
      this.dataProvider = dataProvider;
      return this;
    }

    public CloudEventGenerator build() {
      if (this.dataProvider == null) {
        String capturedDataType = this.dataType;
        this.dataProvider = id -> defaultData(id, capturedDataType);
      }
      return new CloudEventGenerator(this);
    }
  }
}