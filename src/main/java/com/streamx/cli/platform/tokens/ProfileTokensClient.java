package com.streamx.cli.platform.tokens;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST client for the profile personal-access-token endpoints. Hand-written (not generated) so the
 * CLI does not depend on regenerating the platform OpenAPI artifact.
 */
@Path("/api/v1/profile/tokens")
public interface ProfileTokensClient {

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  Response create(CreateTokenRequest request);

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  Response list();

  @DELETE
  @Path("/{id}")
  Response delete(@PathParam("id") String id);
}
