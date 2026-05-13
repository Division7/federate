package edu.ucsb.cs156.federate

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.apache.catalina.mapper.Mapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.FactorGrantedAuthority
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.UUID
import kotlin.math.exp

@Configuration
@EnableWebSecurity
class SecurityConfiguration {

    @Bean
    @Order(1)
    fun authorizationSecurityChain(httpSecurity: HttpSecurity): SecurityFilterChain{
        return httpSecurity
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .oauth2AuthorizationServer {
                httpSecurity.securityMatcher(it.endpointsMatcher)
                it.oidc(Customizer.withDefaults())
                it.authorizationEndpoint {
                    it.errorResponseHandler { request, response, exception ->
                        response.sendError(HttpStatus.BAD_REQUEST.value(), exception.message)
                        println("request = ${request}")
                        println("exception = ${exception}")
                        exception.printStackTrace()
                        println("exception message: ${exception.message}")
                    }
                }
            }
            .exceptionHandling {
                it.defaultAuthenticationEntryPointFor(LoginUrlAuthenticationEntryPoint("/oauth2/authorization/google"),
                    MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                )
            }
            .build();

    }

    @Bean
    @Order(2)
    fun oauth2ClientSecurityChain(httpSecurity: HttpSecurity): SecurityFilterChain{
        return httpSecurity
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .oauth2Login {
                it.userInfoEndpoint {
                    it.userAuthoritiesMapper(grantedAuthoritiesMapper())
                }
            }.build();
    }

    @Bean
    fun jwkSource(): JWKSource<SecurityContext> {
        val keyPair = generateRsaKey()
        val publicKey = keyPair.public as RSAPublicKey
        val privateKey = keyPair.private as RSAPrivateKey
        val rsaKey = RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build()
        val jwkSet = JWKSet(rsaKey)
        return ImmutableJWKSet(jwkSet)
    }

    private fun generateRsaKey(): KeyPair = try {
        KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    } catch (ex: Exception) {
        throw IllegalStateException(ex)
    }

    @Bean
    fun jwtDecoder(jwkSource: JWKSource<SecurityContext>): JwtDecoder =
        OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)

    @Bean
    fun authorizationServerSettings(): AuthorizationServerSettings =
        AuthorizationServerSettings.builder().build()

    @Bean
    fun grantedAuthoritiesMapper() : GrantedAuthoritiesMapper {
        return object : GrantedAuthoritiesMapper {
            override fun mapAuthorities(authorities: MutableCollection<out GrantedAuthority>): MutableCollection<out GrantedAuthority> {
                val map = HashSet<GrantedAuthority>()
                map.add(
                    FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.AUTHORIZATION_CODE_AUTHORITY)
                )
                map.addAll(authorities)
                return map
            }
        }
    }

    @Bean
    fun tokenCustomizer(): OAuth2TokenCustomizer<JwtEncodingContext> =
        OAuth2TokenCustomizer { context ->
            if (OidcParameterNames.ID_TOKEN == context.tokenType.value) {
                val principal = context.getPrincipal<Authentication>()
                if (principal is OAuth2AuthenticationToken) {
                    val attributes = principal.principal?.attributes
                    attributes?.get("email")?.let { context.claims.claim("email", it) }
                    attributes?.get("name")?.let { context.claims.claim("name", it) }
                    attributes?.get("picture")?.let { context.claims.claim("picture", it) }
                }
            }
        }
}