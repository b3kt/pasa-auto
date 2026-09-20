package com.github.b3kt.infrastructure.google;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Google's OAuth token endpoint, used to exchange an authorization code for an id token.
 *
 * <p>An interface rather than a hand-rolled HTTP call so tests can replace Google with
 * {@code @InjectMock} instead of talking to the network.
 */
@Path("/token")
@RegisterRestClient(configKey = "google-oauth")
public interface GoogleTokenClient {

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    GoogleTokenResponse exchangeCode(
            @FormParam("code") String code,
            @FormParam("client_id") String clientId,
            @FormParam("client_secret") String clientSecret,
            @FormParam("redirect_uri") String redirectUri,
            @FormParam("code_verifier") String codeVerifier,
            @FormParam("grant_type") String grantType);
}
