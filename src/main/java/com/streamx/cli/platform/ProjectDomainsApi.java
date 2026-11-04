package com.streamx.cli.platform;

import static com.streamx.cli.i18n.MessageProvider.msg;

import com.streamx.cli.framework.CliException;
import com.streamx.cli.platform.generated.api.DomainsApi;
import com.streamx.cli.platform.generated.model.DomainStatus;
import com.streamx.cli.platform.generated.model.DomainsRequest;
import com.streamx.cli.platform.generated.model.ProjectDomains;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectDomainsApi {

  private static final int BAD_REQUEST = 400;
  private static final int NOT_IMPLEMENTED = 501;
  // Field errors name the entry of the request body by its ID, alone or inside a longer path,
  // such as updateProjectDomains.domainsRequest.domains[shop].<map value>.
  private static final Pattern DOMAIN_ID = Pattern.compile("domains(?:<K>)?\\[([^\\]]+)]");

  private final PlatformClients clients;
  private final DomainsApi api;

  public ProjectDomainsApi(PlatformClients clients) {
    this.clients = clients;
    this.api = clients.api(DomainsApi.class);
  }

  public List<ProjectDomainView> list(String orgId, String projectId) {
    return views(call(projectId, () -> clients.call(
        () -> api.getProjectDomains(orgId, projectId, null, null), ProjectDomains.class)));
  }

  /**
   * Replaces the project's domains, hostnames by ID; the answer lists them all, with their DNS
   * records.
   */
  public List<ProjectDomainView> update(String orgId, String projectId,
      Map<String, String> domains) {
    try {
      return views(call(projectId, () -> clients.call(
          () -> api.updateProjectDomains(
              orgId, projectId, new DomainsRequest().domains(domains), null, null),
          ProjectDomains.class)));
    } catch (PlatformClients.RequestFailedException e) {
      if (e.status() == BAD_REQUEST && !e.violations().isEmpty()) {
        throw new CliException(msg.domainsRejected(rejected(e.violations(), domains)), e);
      }
      throw e;
    }
  }

  public void verify(String orgId, String projectId, String domain) {
    call(projectId, () -> {
      clients.call(() -> api.verifyProjectDomain(domain, orgId, projectId, null, null));
      return null;
    });
  }

  /**
   * Checks the domain live: its records in public DNS, as verification does, and HTTPS on the
   * project's StreamX hostnames.
   */
  public DomainStatusView status(String orgId, String projectId, String domain) {
    return DomainStatusView.from(call(projectId, () -> clients.call(
        () -> api.getProjectDomainStatus(domain, orgId, projectId, null, null),
        DomainStatus.class)));
  }

  private static <T> T call(String projectId, Supplier<T> request) {
    try {
      return request.get();
    } catch (PlatformClients.NotFoundException e) {
      throw new CliException(msg.domainsNotFound(projectId), e);
    } catch (PlatformClients.RequestFailedException e) {
      if (e.status() == NOT_IMPLEMENTED) {
        throw new CliException(msg.domainsNotSupported(), e);
      }
      throw e;
    }
  }

  private static List<ProjectDomainView> views(ProjectDomains domains) {
    if (domains == null || domains.getDomains() == null) {
      return List.of();
    }
    return domains.getDomains().stream().map(ProjectDomainView::from).toList();
  }

  static String rejected(List<PlatformClients.Violation> violations,
      Map<String, String> domains) {
    List<String> lines = new ArrayList<>();
    for (PlatformClients.Violation violation : violations) {
      Matcher id = DOMAIN_ID.matcher(violation.field());
      String hostname = id.find() ? domains.get(id.group(1)) : null;
      String subject = hostname == null || hostname.isBlank() ? violation.field() : hostname;
      lines.add("  " + subject + ": " + violation.message());
    }
    return String.join(System.lineSeparator(), lines);
  }
}
