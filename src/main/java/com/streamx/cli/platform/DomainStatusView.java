package com.streamx.cli.platform;

import com.streamx.cli.platform.generated.model.DomainStatus;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
public record DomainStatusView(
    String domain,
    String checkedAt,
    String verification,
    String tls,
    List<DnsRecordCheckView> records
) {

  public static DomainStatusView from(DomainStatus status) {
    return new DomainStatusView(
        status.getDomain(),
        status.getCheckedAt(),
        status.getVerification() == null ? null : status.getVerification().toString(),
        status.getTls() == null ? null : status.getTls().toString(),
        status.getRecords() == null ? List.of()
            : status.getRecords().stream().map(DnsRecordCheckView::from).toList());
  }

  public boolean ready() {
    return "READY".equals(tls);
  }
}
