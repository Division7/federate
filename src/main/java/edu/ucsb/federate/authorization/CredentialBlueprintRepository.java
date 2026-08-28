package edu.ucsb.federate.authorization;

import edu.ucsb.federate.entities.CredentialBlueprint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CredentialBlueprintRepository extends JpaRepository<CredentialBlueprint, Long> {

  Optional<CredentialBlueprint> findByGithubOrganization(String githubOrganization);
}