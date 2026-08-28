package edu.ucsb.federate.authorization;

import edu.ucsb.federate.entities.CredentialBlueprint;
import java.util.Arrays;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/internal/blueprint")
@RestController
public class BlueprintController {

  private final CredentialBlueprintService credentialBlueprintService;

  public BlueprintController(CredentialBlueprintService credentialBlueprintService) {
    this.credentialBlueprintService = credentialBlueprintService;
  }

  public record BlueprintRequest(String[] domainPatterns, String[] repositoryPatterns){
  }

  @PostMapping("")
  public ResponseEntity<CredentialBlueprint> createBlueprint(String githubOrganization, @RequestBody BlueprintRequest blueprintRequest){
    CredentialBlueprint blueprint = credentialBlueprintService.createCredentialBlueprint(githubOrganization, Arrays.stream(blueprintRequest.domainPatterns).toList(), Arrays.stream(blueprintRequest.repositoryPatterns).toList());
    return ResponseEntity.ok(blueprint);
  }

}
