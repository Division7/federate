package edu.ucsb.federate.authentication;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import edu.ucsb.federate.authorization.OIDCTokenCustomizer;
import edu.ucsb.federate.entities.AdminRepository;
import edu.ucsb.federate.entities.ManagerRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Security;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;

@Configuration
public class AuthConfiguration {

  @Value("${federate.token-prefix}")
  private String token_prefix;

  @Bean
  public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(
      UserRepository userRepository, AdminRepository adminRepo, ManagerRepository managerRepo) {
    OidcUserService delegate = new OidcUserService();
    return new LocalOidcUserService(userRepository, delegate, adminRepo, managerRepo);
  }

  /**
   * Inspects and provides AuthenticationPrincipals for internally issued tokens
   */
  @Bean
  public OpaqueTokenIntrospector inspector(ApiKeyRepository keyRepo, AdminRepository adminRepo, ManagerRepository managerRepo) {
    return new ApiKeyTokenInspector(keyRepo, adminRepo, managerRepo);
  }

  /**
   * Decides between asking GitHub Actions to authenticate or internally authenticating.
   *
   * @see <a
   * href="https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/multitenancy.html#oauth2reourceserver-opaqueandjwt">https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/multitenancy.html#oauth2reourceserver-opaqueandjwt</a>
   */
  @Bean
  @Primary
  public AuthenticationManagerResolver<HttpServletRequest> tokenMatcher(JwtDecoder jwtDecoder,
      OpaqueTokenIntrospector inspector) {
    AuthenticationManager jwt = new ProviderManager( new JwtAuthenticationProvider(jwtDecoder));
    AuthenticationManager localToken = new ProviderManager(new OpaqueTokenAuthenticationProvider(inspector));
    return (request) -> {
      String authHeader = request.getHeader("Authorization");
      if(authHeader != null && authHeader.startsWith("Bearer "+token_prefix)) {
        return localToken;
      }
      return jwt;
    };
  }

  @Bean
  @Qualifier("authMatcher")
  public AuthenticationManagerResolver<HttpServletRequest> authorizationServerMatcher(JWKSource<SecurityContext> jwkSource,
      OpaqueTokenIntrospector inspector) {
    AuthenticationManager jwt = new ProviderManager(new JwtAuthenticationProvider(
        OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)));
    AuthenticationManager localToken = new ProviderManager(new OpaqueTokenAuthenticationProvider(inspector));
    return (request) -> {
      String authHeader = request.getHeader("Authorization");
      if(authHeader != null && authHeader.startsWith("Bearer "+token_prefix)) {
        return localToken;
      }
      return jwt;
    };
  }
  /**
   * Stand-in for JwtClaimValidator before permanent implementation
   */
  @Bean
  public OAuth2TokenValidator<Jwt> subValidator(){
    return new JwtClaimValidator<List<String>>(JwtClaimNames.SUB, (claim) -> true);
  }

  @Bean
  public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer(){
    return new OIDCTokenCustomizer();
  }
}
