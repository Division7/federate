package edu.ucsb.federate;

import edu.ucsb.federate.authentication.SidRetrievalStrategyCustomImpl;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.AclAuthorizationStrategyImpl;
import org.springframework.security.acls.domain.ConsoleAuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import org.springframework.security.acls.jdbc.BasicLookupStrategy;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfiguration {

  /**
   * Filter chain specific to Spring Authorization Server
   * Sectioned off to allow for stub accounts and differing settings
   */
  @Bean
  @Order(1)
  public SecurityFilterChain authorizationFilterChain(HttpSecurity http, OAuth2UserService<OidcUserRequest, OidcUser> service) throws Exception {
    http
        .authorizeHttpRequests(
            request -> request.anyRequest().authenticated()
        )
        .httpBasic(AbstractHttpConfigurer::disable)
        .oauth2Login(oidc -> oidc.userInfoEndpoint(info -> info.oidcUserService(service)))
        .oauth2AuthorizationServer(
            authorizationServer -> authorizationServer.oidc(Customizer.withDefaults())
        ).exceptionHandling(
            handler -> handler.defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
            )
        )
        .oauth2ResourceServer(
            resourceServer -> resourceServer.opaqueToken(Customizer.withDefaults())
        );
    return http.build();
  }

  /**
   * Stateful filter chain for internal API endpoints.
   * CSRF is enabled because it's designed to be used by the frontend.
   * OIDC-only to manage authentication.
   */
  @Bean
  @Order(3)
  public SecurityFilterChain statefulFilterChain(HttpSecurity http, OAuth2UserService<OidcUserRequest, OidcUser> service) throws Exception {
    http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .authorizeHttpRequests(auth -> auth.requestMatchers("/api/internal/**").hasRole("USER")
            .anyRequest().permitAll()
        )
        .httpBasic(AbstractHttpConfigurer::disable)
        .oauth2Login(oidc -> oidc.userInfoEndpoint(info -> info.oidcUserService(service)));
    return http.build();
  }

  /**
   * Stateless filter chain for programmatic API endpoints.
   * CSRF protection is disabled because it's designed to be used by bearer endpoints.
   */
  @Bean
  @Order(2)
  public SecurityFilterChain stateless(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .securityMatcher("/api/programmatic/**")
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .httpBasic(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .oauth2ResourceServer(Customizer.withDefaults());
    return http.build();
  }

  @Bean
  public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
    return new JdbcRegisteredClientRepository(jdbcTemplate);
  }

  @Bean
  public OAuth2AuthorizationService oauth2AuthorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository repository) {
    return new JdbcOAuth2AuthorizationService(jdbcTemplate, repository);
  }

  @Bean
  public OAuth2AuthorizationConsentService oauth2AuthorizationConsentService(JdbcTemplate jdbcTemplate, RegisteredClientRepository repository) {
    return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, repository);
  }

  @Bean
  static AclPermissionEvaluator aclPermissionEvaluator(AclService aclService) {
    return new AclPermissionEvaluator(aclService);
  }

  /**
   * @see <a href="https://docs.spring.io/spring-security/reference/servlet/appendix/database-schema.html#_postgresql">https://docs.spring.io/spring-security/reference/servlet/appendix/database-schema.html#_postgresql</a>
   */
  @Bean
  static JdbcMutableAclService aclService(JdbcTemplate dataSource, LookupStrategy lookupStrategy, AclCache aclCache) {
    JdbcMutableAclService service = new JdbcMutableAclService(dataSource.getDataSource(), lookupStrategy, aclCache);
    service.setClassIdentityQuery("select currval(pg_get_serial_sequence('acl_class', 'id'))");
    service.setSidIdentityQuery("select currval(pg_get_serial_sequence('acl_sid', 'id'))");
    return service;
  }

  @Bean
  static AclAuthorizationStrategy aclAuthorizationStrategy() {
    AclAuthorizationStrategyImpl strategy = new AclAuthorizationStrategyImpl(new SimpleGrantedAuthority("ROLE_ADMIN"));
    strategy.setSidRetrievalStrategy(new SidRetrievalStrategyCustomImpl());
    return strategy;
  }

  @Bean
  static PermissionGrantingStrategy permissionGrantingStrategy() {
    return new DefaultPermissionGrantingStrategy(new ConsoleAuditLogger());
  }

  @Bean
  static AclCache aclCache(PermissionGrantingStrategy permissionGrantingStrategy,
      AclAuthorizationStrategy aclAuthorizationStrategy) {
    Cache cache = new ConcurrentMapCache("aclCache");
    return new SpringCacheBasedAclCache(cache, permissionGrantingStrategy, aclAuthorizationStrategy);
  }

  @Bean
  static LookupStrategy lookupStrategy(JdbcTemplate dataSource, AclCache cache,
      AclAuthorizationStrategy aclAuthorizationStrategy, PermissionGrantingStrategy permissionGrantingStrategy) {
    return new BasicLookupStrategy(dataSource.getDataSource(), cache, aclAuthorizationStrategy, permissionGrantingStrategy);
  }


}
