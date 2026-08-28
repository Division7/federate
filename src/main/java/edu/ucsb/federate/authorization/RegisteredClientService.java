package edu.ucsb.federate.authorization;

import edu.ucsb.federate.entities.CredentialBlueprint;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.stereotype.Service;

@Service
public class RegisteredClientService {

  private final CredentialBlueprintRepository credentialBlueprintRepository;
  private final RegisteredClientRepository registeredClientRepository;

  public RegisteredClientService(CredentialBlueprintRepository credentialBlueprintRepository,
      RegisteredClientRepository registeredClientRepository) {
    this.credentialBlueprintRepository = credentialBlueprintRepository;
    this.registeredClientRepository = registeredClientRepository;
  }

//  @PreAuthorize("hasPermission(#id, 'edu.ucsb.federate.authorization.CredentialBlueprint', 'CREATE')")
  public MintedCredential createCredentials(Long id, String domain){
    CredentialBlueprint selectedBlueprint = credentialBlueprintRepository.findById(id).orElseThrow();
    if(selectedBlueprint.getDomains().stream().noneMatch(pattern -> pattern.matches(domain))){
      throw new IllegalArgumentException("Domain does not match any allowable pattern");
    }

    String clientId = UUID.randomUUID().toString();
    byte[] clientSecret = new byte [32];
    SecureRandom random = new SecureRandom();
    random.nextBytes(clientSecret);
    PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    String base64EncodedSecret = Base64.getEncoder().encodeToString(clientSecret);

    RegisteredClient newClient = RegisteredClient.withId(clientId)
        .clientId(clientId)
        .clientSecret(passwordEncoder.encode(base64EncodedSecret))
        .redirectUri(domain)
        .authorizationGrantTypes((types) -> types.addAll(Set.of(AuthorizationGrantType.AUTHORIZATION_CODE, AuthorizationGrantType.REFRESH_TOKEN)))
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .scopes((scopes) -> scopes.addAll(Set.of("openid", "profile")))
        .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).requireProofKey(false).build())
        .build();
    registeredClientRepository.save(newClient);

    return new MintedCredential(clientId, base64EncodedSecret);
  }

  public static record MintedCredential(String clientId, String clientSecret) {

  }
}
