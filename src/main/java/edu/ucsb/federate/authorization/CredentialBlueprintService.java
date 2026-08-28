package edu.ucsb.federate.authorization;

import com.google.re2j.Pattern;
import edu.ucsb.federate.authentication.User;
import edu.ucsb.federate.entities.CredentialBlueprint;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.CumulativePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

@Service
public class CredentialBlueprintService {

  private final JdbcMutableAclService aclService;
  private final CredentialBlueprintRepository credentialBlueprintRepository;


  public CredentialBlueprintService(JdbcMutableAclService aclService, CredentialBlueprintRepository credentialBlueprintRepository) {
    this.aclService = aclService;
    this.credentialBlueprintRepository = credentialBlueprintRepository;
  }

//  @PreAuthorize("hasRole('MANAGER')")
  @Transactional
  public CredentialBlueprint createCredentialBlueprint(String githubOrganization, List<String> domainRegex, List<String> matchingRepositories) {
    Permission p = new CumulativePermission()
        .set(BasePermission.ADMINISTRATION)
        .set(BasePermission.READ)
        .set(BasePermission.WRITE)
        .set(BasePermission.DELETE)
        .set(BasePermission.CREATE);

    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    List<Pattern> domainRegexes = domainRegex.stream()
        .map(Pattern::compile)
        .toList();

    List<Pattern> repoRegexes = matchingRepositories.stream()
        .map(Pattern::compile)
        .toList();

    CredentialBlueprint newBlueprint = CredentialBlueprint.builder()
        .creator(user.toEntity())
        .githubOrganization(githubOrganization)
        .domains(domainRegexes)
        .repos(repoRegexes)
        .build();

    credentialBlueprintRepository.save(newBlueprint);

    ObjectIdentity oi = new ObjectIdentityImpl(CredentialBlueprint.class, newBlueprint.getId());

    MutableAcl acl = aclService.createAcl(oi);
    acl.insertAce(0, p, user.getSid(), true);
    aclService.updateAcl(acl);
    return newBlueprint;
  }
}
