package edu.ucsb.federate.authorization;

import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;
import edu.ucsb.federate.authentication.User;
import edu.ucsb.federate.entities.CredentialBlueprint;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.SneakyThrows;
import org.apache.hc.core5.concurrent.CompletedFuture;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
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
  private final CredentialBlueprintService selfBlueprintService;

  public record RegexResult(List<Pattern> compiledPatterns, LinkedHashMap<String, String> failedPatterns) {}

  public CredentialBlueprintService(JdbcMutableAclService aclService, CredentialBlueprintRepository credentialBlueprintRepository,
      @Lazy CredentialBlueprintService selfBlueprintService) {
    this.aclService = aclService;
    this.credentialBlueprintRepository = credentialBlueprintRepository;
    this.selfBlueprintService = selfBlueprintService;
  }

  @PreAuthorize("hasRole('MANAGER')")
  @Transactional
  @SneakyThrows //TODO: Come up with a better way to handle exceptions
  public CredentialBlueprint createCredentialBlueprint(String githubOrganization, List<String> domainRegex, List<String> matchingRepositories) {
    Permission p = new CumulativePermission()
        .set(BasePermission.ADMINISTRATION)
        .set(BasePermission.READ)
        .set(BasePermission.WRITE)
        .set(BasePermission.DELETE)
        .set(BasePermission.CREATE);

    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    CompletableFuture<RegexResult> domainRegexes = selfBlueprintService.compileRegexes(domainRegex);
    CompletableFuture<RegexResult> repoRegexes = selfBlueprintService.compileRegexes(matchingRepositories);

    CompletableFuture.allOf(domainRegexes, repoRegexes).join();

    List<String> failures = new ArrayList<>();
    failures.addAll(domainRegexes.get().failedPatterns().values());
    failures.addAll(repoRegexes.get().failedPatterns().values());

    if(!failures.isEmpty()) {
      StringBuilder failureMessage = new StringBuilder();
      failureMessage.append("Failed to compile regexes: ");
      for (String failure : failures) {
        failureMessage.append(failure).append(", ");
      }
      failureMessage.delete(failureMessage.length() - 2, failureMessage.length());
      throw new IllegalArgumentException(failureMessage.toString());
    }

    CredentialBlueprint newBlueprint = CredentialBlueprint.builder()
        .creator(user.toEntity())
        .githubOrganization(githubOrganization)
        .domains(domainRegexes.get().compiledPatterns())
        .repos(repoRegexes.get().compiledPatterns())
        .build();

    credentialBlueprintRepository.save(newBlueprint);

    ObjectIdentity oi = new ObjectIdentityImpl(CredentialBlueprint.class, newBlueprint.getId());

    MutableAcl acl = aclService.createAcl(oi);
    acl.insertAce(0, p, user.getSid(), true);
    aclService.updateAcl(acl);
    return newBlueprint;
  }

  public CompletableFuture<RegexResult> compileRegexes(List<String> toCompile) {
    return CompletableFuture.supplyAsync(() -> {
      RegexResult result = new RegexResult(new ArrayList<>(), new LinkedHashMap<>());
      for(String regex : toCompile) {
        try {
          result.compiledPatterns.add(Pattern.compile(regex));
        } catch (PatternSyntaxException e) {
          result.failedPatterns.put(regex, "regex compilation failed on " + e.getPattern() + ": " + e.getMessage());
        }
      }
      return result;
    });
  }
}
