package edu.ucsb.federate.authorization;

import edu.ucsb.federate.authentication.LocalOidcUser;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

public class OIDCTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

  @Override
  public void customize(JwtEncodingContext context) {
    if(OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())){
     if(context.getPrincipal() instanceof LocalOidcUser){
      context.getClaims().claims(
          claims -> {
            claims.putAll(
                ((LocalOidcUser) context.getPrincipal()).getUserInfo().getClaims()
            );
            claims.put("role", context.getPrincipal().getAuthorities().stream().map(
                GrantedAuthority::getAuthority).collect(Collectors.toSet()));
          }
      );
     }
    }
  }
}
