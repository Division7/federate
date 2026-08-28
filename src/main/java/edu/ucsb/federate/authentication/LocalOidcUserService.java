package edu.ucsb.federate.authentication;

import edu.ucsb.federate.entities.AdminRepository;
import edu.ucsb.federate.entities.ManagerRepository;
import edu.ucsb.federate.entities.UserEntity;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

  public class LocalOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegate;
    private final UserRepository userRepository;
    private final AdminRepository adminRepo;
    private final ManagerRepository managerRepo;

    public LocalOidcUserService(UserRepository userRepository, OidcUserService delegate,
        AdminRepository adminRepo, ManagerRepository managerRepo) {
      this.userRepository = userRepository;
      this.delegate = delegate;
      this.adminRepo = adminRepo;
      this.managerRepo = managerRepo;
    }


    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
      OidcUser loadedUser = delegate.loadUser(userRequest);

      Optional<UserEntity> localUser = userRepository.findByGoogleSub(loadedUser.getSubject());

      Set<GrantedAuthority> authorities = new HashSet<>(loadedUser.getAuthorities());

      if(adminRepo.existsById(loadedUser.getEmail())){
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
      }

      if(managerRepo.existsById(loadedUser.getEmail())){
        authorities.add(new SimpleGrantedAuthority("ROLE_MANAGER"));
      }

      authorities.add(FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.AUTHORIZATION_CODE_AUTHORITY));

      LocalOidcUser user;
      if(localUser.isPresent()){
        user = new LocalOidcUser(authorities, loadedUser.getIdToken(), loadedUser.getUserInfo(),
            localUser.get());
        if(!user.matches(localUser.get())){
          userRepository.save(user.toEntity());
        }
      }else{
        user = new LocalOidcUser(authorities, loadedUser.getIdToken(), loadedUser.getUserInfo());
        UserEntity savedUser = user.toEntity();
        userRepository.save(savedUser);
        user.setId(savedUser.getId());
      }
      return user;
    }
  }