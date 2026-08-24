package edu.ucsb.federate.authentication;

import edu.ucsb.federate.entities.UserEntity;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

  public class LocalOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegate;
    private final UserRepository userRepository;

    public LocalOidcUserService(UserRepository userRepository, OidcUserService delegate) {
      this.userRepository = userRepository;
      this.delegate = delegate;
    }


    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
      OidcUser loadedUser = delegate.loadUser(userRequest);

      Optional<UserEntity> localUser = userRepository.findByGoogleSub(loadedUser.getSubject());

      Set<GrantedAuthority> authorities = new HashSet<>(loadedUser.getAuthorities());
      authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

      LocalOidcUser user;
      if(localUser.isPresent()){
        user = new LocalOidcUser(authorities, loadedUser.getIdToken(), loadedUser.getUserInfo(),
            localUser.get());
        if(!user.matches(localUser.get())){
          userRepository.save(user.toEntity());
        }
      }else{
        user = new LocalOidcUser(authorities, loadedUser.getIdToken(), loadedUser.getUserInfo());
        userRepository.save(user.toEntity());
      }
      return user;
    }
  }