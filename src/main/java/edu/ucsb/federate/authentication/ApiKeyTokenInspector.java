package edu.ucsb.federate.authentication;

import edu.ucsb.federate.entities.AdminRepository;
import edu.ucsb.federate.entities.ManagerRepository;
import edu.ucsb.federate.entities.UserEntity;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.transaction.annotation.Transactional;

public class ApiKeyTokenInspector implements OpaqueTokenIntrospector {


  private final ApiKeyRepository keyRepo;
  private final AdminRepository adminRepo;
  private final ManagerRepository managerRepo;

  public ApiKeyTokenInspector(ApiKeyRepository repository, AdminRepository adminRepo,
      ManagerRepository managerRepo){
    this.keyRepo = repository;
    this.adminRepo = adminRepo;
    this.managerRepo = managerRepo;
  }

  @Override
  @Transactional
  public OAuth2AuthenticatedPrincipal introspect(String token) {
    String hash;
    try{
      hash = DigestUtils.sha256Hex(Base64.getDecoder().decode(token.getBytes()));
    }catch(IllegalArgumentException e){
      throw new OAuth2IntrospectionException("Invalid bearer token");
    }
    ApiKey key = keyRepo.findById(hash).orElseThrow(() -> new BadOpaqueTokenException("Invalid bearer token"));

    UserEntity user = key.getOwner();

    Set<GrantedAuthority> authorities = new HashSet<>();

    if(adminRepo.existsById(user.getEmail()))
      authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

    if(managerRepo.existsById(user.getEmail()))
      authorities.add(new SimpleGrantedAuthority("ROLE_MANAGER"));

    authorities.add(new SimpleGrantedAuthority("ROLE_STATELESS"));

    return new ApiKeyUser(user, authorities);
  }
}
