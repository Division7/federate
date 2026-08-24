package edu.ucsb.federate.authorization;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

@Service
public class CredentialBlueprintService {

  private final RegisteredClientRepository registeredClientRepository;


  public CredentialBlueprintService(RegisteredClientRepository registeredClientRepository) {
    this.registeredClientRepository = registeredClientRepository;
  }

  @PreAuthorize("hasRole('ROLE_MANAGER')")
  public void createCredentialBlueprint(String githubOrganization, List<String> domainRegex, List<String> matchingRepositories) {
    Permission p = null;


  }
}
