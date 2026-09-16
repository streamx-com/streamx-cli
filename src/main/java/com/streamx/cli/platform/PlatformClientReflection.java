package com.streamx.cli.platform;

import com.streamx.cli.platform.generated.model.AllowedListeners;
import com.streamx.cli.platform.generated.model.AllowedListenersNamespaces;
import com.streamx.cli.platform.generated.model.AllowedListenersNamespacesSelector;
import com.streamx.cli.platform.generated.model.AllowedListenersNamespacesSelectorMatchExpressionsInner;
import com.streamx.cli.platform.generated.model.AllowedRoutes;
import com.streamx.cli.platform.generated.model.AllowedRoutesKindsInner;
import com.streamx.cli.platform.generated.model.AutoRef;
import com.streamx.cli.platform.generated.model.AutoRefUsing;
import com.streamx.cli.platform.generated.model.BackendObjectReference;
import com.streamx.cli.platform.generated.model.Channel;
import com.streamx.cli.platform.generated.model.ChannelMetadata;
import com.streamx.cli.platform.generated.model.ChannelType;
import com.streamx.cli.platform.generated.model.Channels;
import com.streamx.cli.platform.generated.model.Cluster;
import com.streamx.cli.platform.generated.model.ClusterLocation;
import com.streamx.cli.platform.generated.model.Clusters;
import com.streamx.cli.platform.generated.model.ClustersProcessingInner;
import com.streamx.cli.platform.generated.model.Container;
import com.streamx.cli.platform.generated.model.ContainerDescriptor;
import com.streamx.cli.platform.generated.model.ContainerDescriptorAutoRefInner;
import com.streamx.cli.platform.generated.model.ContainerDescriptorIncomingValue;
import com.streamx.cli.platform.generated.model.ContainerDescriptorOutgoingValue;
import com.streamx.cli.platform.generated.model.ContainerStatus;
import com.streamx.cli.platform.generated.model.CookieConfig;
import com.streamx.cli.platform.generated.model.CreatePersonalAccessTokenRequest;
import com.streamx.cli.platform.generated.model.CreateProjectRepositoryRequest;
import com.streamx.cli.platform.generated.model.CreateProjectRequest;
import com.streamx.cli.platform.generated.model.CreateProjectRequestRepository;
import com.streamx.cli.platform.generated.model.DataFilterParams;
import com.streamx.cli.platform.generated.model.EncryptedPayload;
import com.streamx.cli.platform.generated.model.EncryptionRequest;
import com.streamx.cli.platform.generated.model.EnvironmentFrom;
import com.streamx.cli.platform.generated.model.ErrorResponse;
import com.streamx.cli.platform.generated.model.EventData;
import com.streamx.cli.platform.generated.model.FindData200Response;
import com.streamx.cli.platform.generated.model.ForwardBodyConfig;
import com.streamx.cli.platform.generated.model.Fraction;
import com.streamx.cli.platform.generated.model.FrontendTLSConfig;
import com.streamx.cli.platform.generated.model.FrontendTLSConfigDefault;
import com.streamx.cli.platform.generated.model.FrontendTLSConfigDefaultValidation;
import com.streamx.cli.platform.generated.model.FrontendTLSConfigDefaultValidationCaCertificateRefsInner;
import com.streamx.cli.platform.generated.model.FrontendTLSConfigPerPortInner;
import com.streamx.cli.platform.generated.model.FrontendTLSValidation;
import com.streamx.cli.platform.generated.model.GRPCAuthConfig;
import com.streamx.cli.platform.generated.model.GatewayBackendTLS;
import com.streamx.cli.platform.generated.model.GatewayInfrastructure;
import com.streamx.cli.platform.generated.model.GatewaySpec;
import com.streamx.cli.platform.generated.model.GatewaySpecAddress;
import com.streamx.cli.platform.generated.model.GatewaySpecAllowedListeners;
import com.streamx.cli.platform.generated.model.GatewaySpecInfrastructure;
import com.streamx.cli.platform.generated.model.GatewaySpecListenersInner;
import com.streamx.cli.platform.generated.model.GatewaySpecListenersInnerAllowedRoutes;
import com.streamx.cli.platform.generated.model.GatewaySpecListenersInnerTls;
import com.streamx.cli.platform.generated.model.GatewaySpecTls;
import com.streamx.cli.platform.generated.model.GatewaySpecTlsBackend;
import com.streamx.cli.platform.generated.model.GatewaySpecTlsFrontend;
import com.streamx.cli.platform.generated.model.GatewayTLSConfig;
import com.streamx.cli.platform.generated.model.GetMetrics200Response;
import com.streamx.cli.platform.generated.model.GetMetrics400Response;
import com.streamx.cli.platform.generated.model.GetMetrics400ResponseViolationsInner;
import com.streamx.cli.platform.generated.model.HTTPAuthConfig;
import com.streamx.cli.platform.generated.model.HTTPBackendRef;
import com.streamx.cli.platform.generated.model.HTTPCORSFilter;
import com.streamx.cli.platform.generated.model.HTTPExternalAuthFilter;
import com.streamx.cli.platform.generated.model.HTTPHeader;
import com.streamx.cli.platform.generated.model.HTTPHeaderFilter;
import com.streamx.cli.platform.generated.model.HTTPHeaderMatch;
import com.streamx.cli.platform.generated.model.HTTPPathMatch;
import com.streamx.cli.platform.generated.model.HTTPPathModifier;
import com.streamx.cli.platform.generated.model.HTTPQueryParamMatch;
import com.streamx.cli.platform.generated.model.HTTPRequestMirrorFilter;
import com.streamx.cli.platform.generated.model.HTTPRequestRedirectFilter;
import com.streamx.cli.platform.generated.model.HTTPRouteFilter;
import com.streamx.cli.platform.generated.model.HTTPRouteMatch;
import com.streamx.cli.platform.generated.model.HTTPRouteRetry;
import com.streamx.cli.platform.generated.model.HTTPRouteRule;
import com.streamx.cli.platform.generated.model.HTTPRouteSpec;
import com.streamx.cli.platform.generated.model.HTTPRouteTimeouts;
import com.streamx.cli.platform.generated.model.HTTPURLRewriteFilter;
import com.streamx.cli.platform.generated.model.IncomingChannel;
import com.streamx.cli.platform.generated.model.IncomingChannelDescriptor;
import com.streamx.cli.platform.generated.model.IngestionService;
import com.streamx.cli.platform.generated.model.IngestionServiceContainer;
import com.streamx.cli.platform.generated.model.IngestionServiceContainersValue;
import com.streamx.cli.platform.generated.model.IngestionServiceContainersValueEnvironmentFrom;
import com.streamx.cli.platform.generated.model.IngestionServiceVolumesValue;
import com.streamx.cli.platform.generated.model.IngestionServiceVolumesValueSize;
import com.streamx.cli.platform.generated.model.InitStateMode;
import com.streamx.cli.platform.generated.model.Invitation;
import com.streamx.cli.platform.generated.model.InvitationAccept;
import com.streamx.cli.platform.generated.model.InvitationRequest;
import com.streamx.cli.platform.generated.model.InvitationRole;
import com.streamx.cli.platform.generated.model.LabelSelector;
import com.streamx.cli.platform.generated.model.LabelSelectorRequirement;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValue;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueParentRefsInner;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInner;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerBackendRefsInner;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInner;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerCors;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerExtensionRef;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerExternalAuth;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerExternalAuthBackendRef;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerExternalAuthForwardBody;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerExternalAuthGrpc;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerExternalAuthHttp;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerRequestHeaderModifier;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerRequestHeaderModifierAddInner;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerRequestMirror;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerRequestMirrorFraction;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerRequestRedirect;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerRequestRedirectPath;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerUrlRewrite;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerMatchesInner;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerMatchesInnerHeadersInner;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerMatchesInnerPath;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerRetry;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerSessionPersistence;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerSessionPersistenceCookieConfig;
import com.streamx.cli.platform.generated.model.ListRoutes200ResponseValueRulesInnerTimeouts;
import com.streamx.cli.platform.generated.model.Listener;
import com.streamx.cli.platform.generated.model.ListenerNamespaces;
import com.streamx.cli.platform.generated.model.ListenerTLSConfig;
import com.streamx.cli.platform.generated.model.ListingDataPage;
import com.streamx.cli.platform.generated.model.ListingDataPageFilters;
import com.streamx.cli.platform.generated.model.LocalObjectReference;
import com.streamx.cli.platform.generated.model.LocalParametersReference;
import com.streamx.cli.platform.generated.model.Location;
import com.streamx.cli.platform.generated.model.MeshDefaults;
import com.streamx.cli.platform.generated.model.MeshDefaultsService;
import com.streamx.cli.platform.generated.model.MetricsParamResultType;
import com.streamx.cli.platform.generated.model.Name;
import com.streamx.cli.platform.generated.model.NameAndRole;
import com.streamx.cli.platform.generated.model.Networking;
import com.streamx.cli.platform.generated.model.NetworkingGatewaysValue;
import com.streamx.cli.platform.generated.model.ObjectReference;
import com.streamx.cli.platform.generated.model.Organization;
import com.streamx.cli.platform.generated.model.OutgoingChannel;
import com.streamx.cli.platform.generated.model.OutgoingChannelDescriptor;
import com.streamx.cli.platform.generated.model.ParentReference;
import com.streamx.cli.platform.generated.model.PendingChange;
import com.streamx.cli.platform.generated.model.PersonalAccessTokenResponse;
import com.streamx.cli.platform.generated.model.PersonalAccessTokenSummary;
import com.streamx.cli.platform.generated.model.PodStatus;
import com.streamx.cli.platform.generated.model.PrivateKeyRequest;
import com.streamx.cli.platform.generated.model.PrivatePublicKeyPair;
import com.streamx.cli.platform.generated.model.Profile;
import com.streamx.cli.platform.generated.model.ProfileUpdate;
import com.streamx.cli.platform.generated.model.Project;
import com.streamx.cli.platform.generated.model.ProjectRepository;
import com.streamx.cli.platform.generated.model.ProjectRepositoryProjectRepositoryStatus;
import com.streamx.cli.platform.generated.model.ProjectRepositoryRequest;
import com.streamx.cli.platform.generated.model.ProjectRepositoryStatus;
import com.streamx.cli.platform.generated.model.ProjectRequest;
import com.streamx.cli.platform.generated.model.ProjectStatus;
import com.streamx.cli.platform.generated.model.ProjectStatusStatusesInner;
import com.streamx.cli.platform.generated.model.PublicKey;
import com.streamx.cli.platform.generated.model.Quantity;
import com.streamx.cli.platform.generated.model.Replica;
import com.streamx.cli.platform.generated.model.Replicas;
import com.streamx.cli.platform.generated.model.ReplicasInstancesInner;
import com.streamx.cli.platform.generated.model.RepositorySettings;
import com.streamx.cli.platform.generated.model.RepositoryValidationRequest;
import com.streamx.cli.platform.generated.model.Role;
import com.streamx.cli.platform.generated.model.RoleChange;
import com.streamx.cli.platform.generated.model.RouteGroupKind;
import com.streamx.cli.platform.generated.model.RouteNamespaces;
import com.streamx.cli.platform.generated.model.SecretObjectReference;
import com.streamx.cli.platform.generated.model.Service;
import com.streamx.cli.platform.generated.model.ServiceContainer;
import com.streamx.cli.platform.generated.model.ServiceContainersValue;
import com.streamx.cli.platform.generated.model.ServiceContainersValueIncomingValue;
import com.streamx.cli.platform.generated.model.ServiceContainersValueOutgoingValue;
import com.streamx.cli.platform.generated.model.ServiceDefaults;
import com.streamx.cli.platform.generated.model.ServiceDescriptor;
import com.streamx.cli.platform.generated.model.ServiceDescriptorContainersValue;
import com.streamx.cli.platform.generated.model.ServiceDetails;
import com.streamx.cli.platform.generated.model.ServiceDetailsChannels;
import com.streamx.cli.platform.generated.model.ServiceDetailsContainersInner;
import com.streamx.cli.platform.generated.model.ServiceDetailsReplicas;
import com.streamx.cli.platform.generated.model.ServiceListItem;
import com.streamx.cli.platform.generated.model.ServiceMesh;
import com.streamx.cli.platform.generated.model.ServiceMeshDefault;
import com.streamx.cli.platform.generated.model.ServiceMeshDescriptorsValue;
import com.streamx.cli.platform.generated.model.ServiceMeshIngestionValue;
import com.streamx.cli.platform.generated.model.ServiceMeshNetworking;
import com.streamx.cli.platform.generated.model.ServiceMeshProcessingValue;
import com.streamx.cli.platform.generated.model.ServiceMeshSourcesValue;
import com.streamx.cli.platform.generated.model.ServiceReplicaDetails;
import com.streamx.cli.platform.generated.model.ServiceType;
import com.streamx.cli.platform.generated.model.SessionPersistence;
import com.streamx.cli.platform.generated.model.Source;
import com.streamx.cli.platform.generated.model.Source1;
import com.streamx.cli.platform.generated.model.SourceMetadata;
import com.streamx.cli.platform.generated.model.SourceStatus;
import com.streamx.cli.platform.generated.model.SourceToken;
import com.streamx.cli.platform.generated.model.State;
import com.streamx.cli.platform.generated.model.Status;
import com.streamx.cli.platform.generated.model.TLSConfig;
import com.streamx.cli.platform.generated.model.TLSPortConfig;
import com.streamx.cli.platform.generated.model.User;
import com.streamx.cli.platform.generated.model.UserStatus;
import com.streamx.cli.platform.generated.model.ValidationError;
import com.streamx.cli.platform.generated.model.ValidationResult;
import com.streamx.cli.platform.generated.model.Violation;
import com.streamx.cli.platform.generated.model.Volume;
import com.streamx.cli.platform.generated.model.VolumesFrom;
import io.quarkus.runtime.annotations.RegisterForReflection;

// The generated client returns raw Response bodies (return-response=true), so the models never
// appear in client method signatures and the extension does not register them for reflection.
// PlatformClients deserializes them with a plain ObjectMapper, which needs this registration
// in the native image. Regenerate the list from
// target/generated-sources/open-api/com/streamx/cli/platform/generated/model/*.java
// whenever the aggregated spec changes.
@RegisterForReflection(targets = {
    AllowedListeners.class,
    AllowedListenersNamespaces.class,
    AllowedListenersNamespacesSelector.class,
    AllowedListenersNamespacesSelectorMatchExpressionsInner.class,
    AllowedRoutes.class,
    AllowedRoutesKindsInner.class,
    AutoRef.class,
    AutoRefUsing.class,
    BackendObjectReference.class,
    Channel.class,
    ChannelMetadata.class,
    ChannelType.class,
    Channels.class,
    Cluster.class,
    ClusterLocation.class,
    Clusters.class,
    ClustersProcessingInner.class,
    Container.class,
    ContainerDescriptor.class,
    ContainerDescriptorAutoRefInner.class,
    ContainerDescriptorIncomingValue.class,
    ContainerDescriptorOutgoingValue.class,
    ContainerStatus.class,
    CookieConfig.class,
    CreatePersonalAccessTokenRequest.class,
    CreateProjectRepositoryRequest.class,
    CreateProjectRequest.class,
    CreateProjectRequestRepository.class,
    DataFilterParams.class,
    EncryptedPayload.class,
    EncryptionRequest.class,
    EnvironmentFrom.class,
    ErrorResponse.class,
    EventData.class,
    FindData200Response.class,
    ForwardBodyConfig.class,
    Fraction.class,
    FrontendTLSConfig.class,
    FrontendTLSConfigDefault.class,
    FrontendTLSConfigDefaultValidation.class,
    FrontendTLSConfigDefaultValidationCaCertificateRefsInner.class,
    FrontendTLSConfigPerPortInner.class,
    FrontendTLSValidation.class,
    GRPCAuthConfig.class,
    GatewayBackendTLS.class,
    GatewayInfrastructure.class,
    GatewaySpec.class,
    GatewaySpecAddress.class,
    GatewaySpecAllowedListeners.class,
    GatewaySpecInfrastructure.class,
    GatewaySpecListenersInner.class,
    GatewaySpecListenersInnerAllowedRoutes.class,
    GatewaySpecListenersInnerTls.class,
    GatewaySpecTls.class,
    GatewaySpecTlsBackend.class,
    GatewaySpecTlsFrontend.class,
    GatewayTLSConfig.class,
    GetMetrics200Response.class,
    GetMetrics400Response.class,
    GetMetrics400ResponseViolationsInner.class,
    HTTPAuthConfig.class,
    HTTPBackendRef.class,
    HTTPCORSFilter.class,
    HTTPExternalAuthFilter.class,
    HTTPHeader.class,
    HTTPHeaderFilter.class,
    HTTPHeaderMatch.class,
    HTTPPathMatch.class,
    HTTPPathModifier.class,
    HTTPQueryParamMatch.class,
    HTTPRequestMirrorFilter.class,
    HTTPRequestRedirectFilter.class,
    HTTPRouteFilter.class,
    HTTPRouteMatch.class,
    HTTPRouteRetry.class,
    HTTPRouteRule.class,
    HTTPRouteSpec.class,
    HTTPRouteTimeouts.class,
    HTTPURLRewriteFilter.class,
    IncomingChannel.class,
    IncomingChannelDescriptor.class,
    IngestionService.class,
    IngestionServiceContainer.class,
    IngestionServiceContainersValue.class,
    IngestionServiceContainersValueEnvironmentFrom.class,
    IngestionServiceVolumesValue.class,
    IngestionServiceVolumesValueSize.class,
    InitStateMode.class,
    Invitation.class,
    InvitationAccept.class,
    InvitationRequest.class,
    InvitationRole.class,
    LabelSelector.class,
    LabelSelectorRequirement.class,
    ListRoutes200ResponseValue.class,
    ListRoutes200ResponseValueParentRefsInner.class,
    ListRoutes200ResponseValueRulesInner.class,
    ListRoutes200ResponseValueRulesInnerBackendRefsInner.class,
    ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInner.class,
    ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerCors.class,
    ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerExtensionRef.class,
    ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerExternalAuth.class,
    ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerExternalAuthBackendRef.class,
    ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerExternalAuthForwardBody.class,
    ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerExternalAuthGrpc.class,
    ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerExternalAuthHttp.class,
    ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerRequestHeaderModifier.class,
    ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerRequestHeaderModifierAddInner
        .class,
    ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerRequestMirror.class,
    ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerRequestMirrorFraction.class,
    ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerRequestRedirect.class,
    ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerRequestRedirectPath.class,
    ListRoutes200ResponseValueRulesInnerBackendRefsInnerFiltersInnerUrlRewrite.class,
    ListRoutes200ResponseValueRulesInnerMatchesInner.class,
    ListRoutes200ResponseValueRulesInnerMatchesInnerHeadersInner.class,
    ListRoutes200ResponseValueRulesInnerMatchesInnerPath.class,
    ListRoutes200ResponseValueRulesInnerRetry.class,
    ListRoutes200ResponseValueRulesInnerSessionPersistence.class,
    ListRoutes200ResponseValueRulesInnerSessionPersistenceCookieConfig.class,
    ListRoutes200ResponseValueRulesInnerTimeouts.class,
    Listener.class,
    ListenerNamespaces.class,
    ListenerTLSConfig.class,
    ListingDataPage.class,
    ListingDataPageFilters.class,
    LocalObjectReference.class,
    LocalParametersReference.class,
    Location.class,
    MeshDefaults.class,
    MeshDefaultsService.class,
    MetricsParamResultType.class,
    Name.class,
    NameAndRole.class,
    Networking.class,
    NetworkingGatewaysValue.class,
    ObjectReference.class,
    Organization.class,
    OutgoingChannel.class,
    OutgoingChannelDescriptor.class,
    ParentReference.class,
    PendingChange.class,
    PersonalAccessTokenResponse.class,
    PersonalAccessTokenSummary.class,
    PodStatus.class,
    PrivateKeyRequest.class,
    PrivatePublicKeyPair.class,
    Profile.class,
    ProfileUpdate.class,
    Project.class,
    ProjectRepository.class,
    ProjectRepositoryProjectRepositoryStatus.class,
    ProjectRepositoryRequest.class,
    ProjectRepositoryStatus.class,
    ProjectRequest.class,
    ProjectStatus.class,
    ProjectStatusStatusesInner.class,
    PublicKey.class,
    Quantity.class,
    Replica.class,
    Replicas.class,
    ReplicasInstancesInner.class,
    RepositorySettings.class,
    RepositoryValidationRequest.class,
    Role.class,
    RoleChange.class,
    RouteGroupKind.class,
    RouteNamespaces.class,
    SecretObjectReference.class,
    Service.class,
    ServiceContainer.class,
    ServiceContainersValue.class,
    ServiceContainersValueIncomingValue.class,
    ServiceContainersValueOutgoingValue.class,
    ServiceDefaults.class,
    ServiceDescriptor.class,
    ServiceDescriptorContainersValue.class,
    ServiceDetails.class,
    ServiceDetailsChannels.class,
    ServiceDetailsContainersInner.class,
    ServiceDetailsReplicas.class,
    ServiceListItem.class,
    ServiceMesh.class,
    ServiceMeshDefault.class,
    ServiceMeshDescriptorsValue.class,
    ServiceMeshIngestionValue.class,
    ServiceMeshNetworking.class,
    ServiceMeshProcessingValue.class,
    ServiceMeshSourcesValue.class,
    ServiceReplicaDetails.class,
    ServiceType.class,
    SessionPersistence.class,
    Source.class,
    Source1.class,
    SourceMetadata.class,
    SourceStatus.class,
    SourceToken.class,
    State.class,
    Status.class,
    TLSConfig.class,
    TLSPortConfig.class,
    User.class,
    UserStatus.class,
    ValidationError.class,
    ValidationResult.class,
    Violation.class,
    Volume.class,
    VolumesFrom.class
})
public class PlatformClientReflection {
}
