package edu.ucsb.federate.authentication;

import edu.ucsb.federate.entities.UserEntity;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.transaction.annotation.Transactional;

public class ApiKeyTokenInspector implements OpaqueTokenIntrospector {


  private final ApiKeyRepository keyRepo;

  public ApiKeyTokenInspector(ApiKeyRepository repository){
    this.keyRepo = repository;
  }

  @Override
  @Transactional
  public OAuth2AuthenticatedPrincipal introspect(String token) {
    String hash = DigestUtils.sha256Hex(Base64.getDecoder().decode(token.getBytes()));
    ApiKey key = keyRepo.findById(hash).orElseThrow(() -> new BadOpaqueTokenException("Invalid bearer token"));

    UserEntity user = key.getOwner();



    return new ApiKeyUser(user, new HashSet<>(
        Set.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_STATELESS"))));
  }
}
