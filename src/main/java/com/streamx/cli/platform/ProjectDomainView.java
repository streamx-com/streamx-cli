package com.streamx.cli.platform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.streamx.cli.platform.generated.model.RecheckDomainDns200Response;
import com.streamx.cli.platform.generated.model.RecheckDomainDns200Response.StateEnum;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectDomainView(
    String id,
    String domain,
    StateEnum state,
    String failureReason,
    Integer attempts,
    String lastCheckedAt,
    String nextCheckAt,
    String verifiedAt,
    List<DnsRecordView> dnsRecords
) {

  public static ProjectDomainView from(RecheckDomainDns200Response domain) {
    return new ProjectDomainView(
        domain.getId(),
        domain.getDomain(),
        domain.getState(),
        domain.getFailureReason(),
        domain.getAttempts(),
        domain.getLastCheckedAt(),
        domain.getNextCheckAt(),
        domain.getVerifiedAt(),
        domain.getDnsRecords() == null ? List.of()
            : domain.getDnsRecords().stream().map(DnsRecordView::from).toList());
  }

  public boolean dnsChecksPassed() {
    return state == StateEnum.VERIFIED;
  }

  public boolean failed() {
    return state == StateEnum.FAILED;
  }
}
