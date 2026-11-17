package com.streamx.cli.platform;

import com.streamx.cli.platform.generated.model.DomainStatusRecordsInner;
import com.streamx.cli.platform.generated.model.DomainStatusRecordsInner.PurposeEnum;
import com.streamx.cli.platform.generated.model.DomainStatusRecordsInner.StatusEnum;
import com.streamx.cli.platform.generated.model.DomainStatusRecordsInner.TypeEnum;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
public record DnsRecordCheckView(
    PurposeEnum purpose,
    TypeEnum type,
    String name,
    List<String> expected,
    StatusEnum status,
    List<String> found
) {

  public static DnsRecordCheckView from(DomainStatusRecordsInner record) {
    return new DnsRecordCheckView(
        record.getPurpose(),
        record.getType(),
        record.getName(),
        record.getExpected() == null ? List.of() : record.getExpected(),
        record.getStatus(),
        record.getFound() == null ? List.of() : record.getFound());
  }

  public boolean inPlace() {
    return status == StatusEnum.DNS_RECORD_MATCHES;
  }
}
