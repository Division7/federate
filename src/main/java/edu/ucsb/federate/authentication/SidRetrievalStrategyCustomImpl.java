package edu.ucsb.federate.authentication;

import java.util.ArrayList;
import java.util.List;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.Authentication;

public class SidRetrievalStrategyCustomImpl implements SidRetrievalStrategy {

  @Override
  public List<Sid> getSids(Authentication authentication) {
    if(authentication == null)
      return List.of();
    List<Sid> sids = new ArrayList<>();
    if(authentication.getPrincipal() instanceof LocalOidcUser){
      sids.add(new PrincipalSid(((LocalOidcUser) authentication.getPrincipal()).getName()));
    }
    authentication.getAuthorities().forEach(authority -> sids.add(new GrantedAuthoritySid(authority)));
    return sids;
  }

}
