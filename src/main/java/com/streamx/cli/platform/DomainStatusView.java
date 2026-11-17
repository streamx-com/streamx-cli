package com.streamx.cli.platform;

import com.streamx.cli.platform.generated.model.DomainStatus;
import com.streamx.cli.platform.generated.model.DomainStatus.DnsChecksEnum;
import com.streamx.cli.platform.generated.model.DomainStatus.TlsCertificateEnum;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
public record DomainStatusView(
    String id,
    String domain,
    String checkedAt,
    DnsChecksEnum dnsChecks,
    TlsCertificateEnum tlsCertificate,
    List<DnsRecordCheckView> records
) {

  public static DomainStatusView from(String id, DomainStatus status) {
    return new DomainStatusView(
        id,
        status.getDomain(),
        status.getCheckedAt(),
        status.getDnsChecks(),
        status.getTlsCertificate(),
        status.getRecords() == null ? List.of()
            : status.getRecords().stream().map(DnsRecordCheckView::from).toList());
  }

  public boolean isSetupComplete() {
    return dnsChecks == DnsChecksEnum.PASSED
        && tlsCertificate == TlsCertificateEnum.TLS_CERTIFICATE_READY
        && records.stream().allMatch(DnsRecordCheckView::inPlace);
  }
}
