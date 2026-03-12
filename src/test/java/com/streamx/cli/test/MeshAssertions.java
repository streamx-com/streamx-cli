package com.streamx.cli.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicLong;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.common.policies.data.TopicStats;

public class MeshAssertions {

  private static final String PULSAR_WEB_URL = "http://localhost:18080";
  private static final String PULSAR_TOPIC = "persistent://streamx/local/inbox.pages";

  private static final AtomicLong prevEventCount = new AtomicLong(0);

  public static synchronized void assertEventsPublished(long count) {
    try (PulsarAdmin admin = PulsarAdmin.builder().serviceHttpUrl(PULSAR_WEB_URL).build()) {
      TopicStats stats = admin.topics().getStats(PULSAR_TOPIC);
      long topicMessageCount = stats.getMsgInCounter();

      long prev = prevEventCount.get();
      assertEquals(count, topicMessageCount - prev);

      prevEventCount.set(topicMessageCount);
    } catch (PulsarAdminException e) {
      throw new AssertionError(
          "Failed to retrieve stats for topic: " + PULSAR_TOPIC + " — " + e.getMessage(), e);
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to create Pulsar admin client: " + e.getMessage(), e);
    }
  }

  public static synchronized void resetPublishedEventsBaseline() {
    try (PulsarAdmin admin = PulsarAdmin.builder().serviceHttpUrl(PULSAR_WEB_URL).build()) {
      TopicStats stats = admin.topics().getStats(PULSAR_TOPIC);
      prevEventCount.set(stats.getMsgInCounter());
    } catch (Exception e) {
      // ignore
    }
  }
}