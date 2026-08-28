package edu.ucsb.federate.authentication;

import com.fasterxml.jackson.annotation.JsonCreator;
import edu.ucsb.federate.entities.UserEntity;
import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;

@Getter
@Setter
public class LocalOidcUser extends DefaultOidcUser implements User {

  @Delegate
  private OidcUserInfo userInfo;
  @NonNull
  private Long id;

  @JsonCreator
  public LocalOidcUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken, OidcUserInfo userInfo, Long id ) {
    super(authorities, idToken, userInfo, IdTokenClaimNames.SUB);
    this.setUserInfo(userInfo);
    this.setId(id);
  }

  public LocalOidcUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken,
      OidcUserInfo userInfo, UserEntity hydrated) {
    super(authorities, idToken, userInfo, IdTokenClaimNames.SUB);
    this.setUserInfo(userInfo);
    this.setId(hydrated.getId());
  }

  public LocalOidcUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken,
      OidcUserInfo userInfo) {
    super(authorities, idToken, userInfo, IdTokenClaimNames.SUB);
    this.setUserInfo(userInfo);
  }


  public boolean matches(UserEntity comparison){
     if (!this.getUserInfo().getPicture().equals(comparison.getPictureUrl())){
       return false;
     }
     else if(!this.getUserInfo().getEmail().equals(comparison.getEmail())){
       return false;
     }
     else if(!this.getUserInfo().getFullName().equals(comparison.getFullName())){
       return false;
     }
     else if(!this.getUserInfo().getGivenName().equals(comparison.getGivenName())){
       return false;
     }
     else if(!this.getUserInfo().getFamilyName().equals(comparison.getFamilyName())){
       return false;
     }
     else if(!this.getSubject().equals(comparison.getGoogleSub())){
       return false;
     }
     return true;
  }

  public UserEntity toEntity(){
    return new UserEntity(this.getId(), this.getUserInfo().getEmail(), this.getUserInfo().getSubject(), this.getUserInfo().getPicture(), this.getUserInfo().getFullName(), this.getUserInfo().getGivenName(), this.getFamilyName(), null);
  }

  @Override
  public String getName(){
    return "local:"+id;
  }

  @Override
  public String getGoogleSub() {
    return this.getSubject();
  }

  @Override
  public String getPictureUrl() {
    return this.getUserInfo() != null ? this.getUserInfo().getPicture() : null;
  }

  public boolean isAdmin(){
    return this.getAuthorities().stream().anyMatch(a -> a.equals(new SimpleGrantedAuthority("ROLE_ADMIN")));
  }

  public boolean isManager(){
    return this.getAuthorities().stream().anyMatch(a -> a.equals(new SimpleGrantedAuthority("ROLE_MANAGER")));
  }

  @Override
  public PrincipalSid getSid(){
    return new PrincipalSid(this.getName());
  }
}