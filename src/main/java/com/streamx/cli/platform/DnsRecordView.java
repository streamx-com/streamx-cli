package com.streamx.cli.platform;

import com.streamx.cli.platform.generated.model.RecheckDomainDns200ResponseDnsRecordsInner;
import com.streamx.cli.platform.generated.model.RecheckDomainDns200ResponseDnsRecordsInner.TypeEnum;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record DnsRecordView(TypeEnum type, String name, String value) {

  public static DnsRecordView from(RecheckDomainDns200ResponseDnsRecordsInner record) {
    return new DnsRecordView(
        record.getType(),
        record.getName(),
        record.getValue());
  }
}
