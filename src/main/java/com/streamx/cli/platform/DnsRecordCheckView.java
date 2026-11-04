package com.streamx.cli.platform;

import com.streamx.cli.platform.generated.model.DomainStatusRecordsInner;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
public record DnsRecordCheckView(
    String purpose,
    String type,
    String name,
    List<String> expected,
    String status,
    List<String> found
) {

  public static DnsRecordCheckView from(DomainStatusRecordsInner record) {
    return new DnsRecordCheckView(
        record.getPurpose() == null ? null : record.getPurpose().toString(),
        record.getType() == null ? null : record.getType().toString(),
        record.getName(),
        record.getExpected() == null ? List.of() : record.getExpected(),
        record.getStatus() == null ? null : record.getStatus().toString(),
        record.getFound() == null ? List.of() : record.getFound());
  }

  public boolean inPlace() {
    return "FOUND".equals(status);
  }
}
