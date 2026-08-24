package edu.ucsb.federate.authentication;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

@Configuration
public class AuthConfiguration {

  @Value("${federate.token-prefix}")
  private String token_prefix;

  @Bean
  public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(
      UserRepository userRepository) {
    OidcUserService delegate = new OidcUserService();
    return new LocalOidcUserService(userRepository, delegate);
  }

  /**
   * Inspects and provides AuthenticationPrincipals for internally issued tokens
   */
  @Bean
  public OpaqueTokenIntrospector inspector(ApiKeyRepository keyRepo) {
    return new ApiKeyTokenInspector(keyRepo);
  }

  /**
   * Decides between asking GitHub Actions to authenticate or internally authenticating.
   *
   * @see <a
   * href="https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/multitenancy.html#oauth2reourceserver-opaqueandjwt">https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/multitenancy.html#oauth2reourceserver-opaqueandjwt</a>
   */
  @Bean
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

  /**
   * Stand-in for JwtClaimValidator before permanent implementation
   */
  @Bean
  public OAuth2TokenValidator<Jwt> subValidator(){
    return new JwtClaimValidator<List<String>>(JwtClaimNames.SUB, (claim) -> true);
  }
}
