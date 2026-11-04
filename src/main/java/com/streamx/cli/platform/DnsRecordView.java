package com.streamx.cli.platform;

import com.streamx.cli.platform.generated.model.VerifyProjectDomain200ResponseDnsRecordsInner;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record DnsRecordView(String type, String name, String value) {

  public static DnsRecordView from(VerifyProjectDomain200ResponseDnsRecordsInner record) {
    return new DnsRecordView(
        record.getType() == null ? null : record.getType().toString(),
        record.getName(),
        record.getValue());
  }
}
