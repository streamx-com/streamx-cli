package com.streamx.cli.platform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.streamx.cli.platform.generated.model.VerifyProjectDomain200Response;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectDomainView(
    String id,
    String domain,
    String state,
    String failureReason,
    Integer attempts,
    String lastCheckedAt,
    String nextCheckAt,
    String verifiedAt,
    List<DnsRecordView> dnsRecords
) {

  public static ProjectDomainView from(VerifyProjectDomain200Response domain) {
    return new ProjectDomainView(
        domain.getId(),
        domain.getDomain(),
        domain.getState() == null ? null : domain.getState().toString(),
        domain.getFailureReason(),
        domain.getAttempts(),
        domain.getLastCheckedAt(),
        domain.getNextCheckAt(),
        domain.getVerifiedAt(),
        domain.getDnsRecords() == null ? List.of()
            : domain.getDnsRecords().stream().map(DnsRecordView::from).toList());
  }

  public boolean verified() {
    return "VERIFIED".equals(state);
  }

  public boolean failed() {
    return "FAILED".equals(state);
  }
}
