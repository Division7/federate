package edu.ucsb.federate.entities.readonly;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

/**
 * Read-only entity for OAuth2 registered client.
 * These entities are managed by {@link org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService}
 * and should not be updated, as this entity was only generated to allow linking to application-specific data via Hibernate.
 */
@Getter
@Setter
@Entity
@Table(name = "oauth2_registered_client")
public class Oauth2RegisteredClient {

  @Id
  @Size(max = 100)
  @Column(name = "id", nullable = false, length = 100)
  private String id;

  @Size(max = 100)
  @NotNull
  @Column(name = "client_id", nullable = false, length = 100)
  private String clientId;

  @NotNull
  @ColumnDefault("now()")
  @Column(name = "client_id_issued_at", nullable = false)
  private Instant clientIdIssuedAt;

  @Size(max = 200)
  @Column(name = "client_secret", length = 200)
  private String clientSecret;

  @Column(name = "client_secret_expires_at")
  private Instant clientSecretExpiresAt;

  @Size(max = 200)
  @NotNull
  @Column(name = "client_name", nullable = false, length = 200)
  private String clientName;

  @Size(max = 1000)
  @NotNull
  @Column(name = "client_authentication_methods", nullable = false, length = 1000)
  private String clientAuthenticationMethods;

  @Size(max = 1000)
  @NotNull
  @Column(name = "authorization_grant_types", nullable = false, length = 1000)
  private String authorizationGrantTypes;

  @Size(max = 1000)
  @Column(name = "redirect_uris", length = 1000)
  private String redirectUris;

  @Size(max = 1000)
  @Column(name = "post_logout_redirect_uris", length = 1000)
  private String postLogoutRedirectUris;

  @Size(max = 1000)
  @NotNull
  @Column(name = "scopes", nullable = false, length = 1000)
  private String scopes;

  @Size(max = 2000)
  @NotNull
  @Column(name = "client_settings", nullable = false, length = 2000)
  private String clientSettings;

  @Size(max = 2000)
  @NotNull
  @Column(name = "token_settings", nullable = false, length = 2000)
  private String tokenSettings;


}