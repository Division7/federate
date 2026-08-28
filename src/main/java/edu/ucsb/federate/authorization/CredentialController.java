package edu.ucsb.federate.authorization;

import edu.ucsb.federate.authorization.RegisteredClientService.MintedCredential;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/credentials")
public class CredentialController {

  private final RegisteredClientService registeredClientService;

  public CredentialController(RegisteredClientService registeredClientService) {
    this.registeredClientService = registeredClientService;
  }

  @PostMapping("")
  public ResponseEntity<?> mintCredential(Long id, String domain){
    try{
      MintedCredential mintedCredential = registeredClientService.createCredentials(id, domain);
      return ResponseEntity.ok(mintedCredential);
    }catch(IllegalArgumentException e){
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }
}
