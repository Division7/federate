package edu.ucsb.federate.authentication;

import edu.ucsb.federate.entities.UserEntity;
import java.util.Collection;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

@Getter
@Setter
public class LocalOidcUser extends DefaultOidcUser implements User {

  @Delegate
  private OidcUserInfo userInfo;
  private Integer githubId;
  @NonNull
  private Long id;

  public LocalOidcUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken,
      OidcUserInfo userInfo, UserEntity hydrated) {
    super(authorities, idToken, userInfo, "sub");
    this.setUserInfo(userInfo);
    this.setGithubId(hydrated.getGithubId());
    this.setId(hydrated.getId());
  }

  public LocalOidcUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken,
      OidcUserInfo userInfo) {
    super(authorities, idToken, userInfo, "sub");
    this.setUserInfo(userInfo);
  }


  public boolean matches(UserEntity comparison){
     if(!this.getGithubId().equals(comparison.getGithubId())){
       return false;
     }
     else if (!this.getUserInfo().getPicture().equals(comparison.getPictureUrl())){
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
    return new UserEntity(this.getId(), this.getUserInfo().getEmail(), this.getUserInfo().getFullName(), this.getUserInfo().getPicture(), this.getUserInfo().getGivenName(), this.getUserInfo().getFamilyName(), this.getSubject(), this.getGithubId());
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
}