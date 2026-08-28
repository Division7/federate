package edu.ucsb.federate.authentication;

import edu.ucsb.federate.entities.UserEntity;
import java.util.Collection;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

@AllArgsConstructor
@Getter
public class ApiKeyUser implements User, OAuth2AuthenticatedPrincipal {

  @Delegate
  private UserEntity user;

  private Collection<? extends GrantedAuthority> authorities;


  @Override
  public Map<String, Object> getAttributes() {
    return Map.of();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getName() {
    return "local:" + this.user.getGoogleSub();
  }

  public boolean isAdmin(){
    return authorities.stream().anyMatch(a -> a.equals(new SimpleGrantedAuthority("ROLE_ADMIN")));
  }

  public boolean isManager(){
    return authorities.stream().anyMatch(a -> a.equals(new SimpleGrantedAuthority("ROLE_MANAGER")));
  }

  public UserEntity toEntity(){
    return this.user;
  }

  public PrincipalSid getSid(){
    return new PrincipalSid(this.getName());
  }
}
