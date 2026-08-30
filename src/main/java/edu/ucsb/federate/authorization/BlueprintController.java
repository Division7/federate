package edu.ucsb.federate.authorization;

import com.google.re2j.PatternSyntaxException;
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
  public ResponseEntity<?> createBlueprint(String githubOrganization, @RequestBody BlueprintRequest blueprintRequest){
    CredentialBlueprint blueprint;
    try {
      blueprint = credentialBlueprintService.createCredentialBlueprint(githubOrganization, Arrays.stream(blueprintRequest.domainPatterns).toList(), Arrays.stream(blueprintRequest.repositoryPatterns).toList());
    } catch (PatternSyntaxException e) {
      return ResponseEntity.badRequest().body("Illegal pattern: " + e.getMessage());
    }
    return ResponseEntity.ok(blueprint);
  }

}
