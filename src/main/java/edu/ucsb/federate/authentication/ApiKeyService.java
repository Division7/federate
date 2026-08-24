package edu.ucsb.federate.authentication;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.security.SecureRandom;
import java.util.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {

  private final ApiKeyRepository apiKeyRepository;

  public ApiKeyService(ApiKeyRepository apiKeyRepository) {
    this.apiKeyRepository = apiKeyRepository;
  }


  public record GeneratedApiKey(String plaintextKey, @JsonIgnore ApiKey key){}
  public GeneratedApiKey generateApiKey(String githubOrganization, LocalOidcUser owner){
    SecureRandom random = new SecureRandom();
    byte[] generatedKey = new byte[128];
    random.nextBytes(generatedKey);
    String key = Base64.getEncoder().encodeToString(generatedKey);
    String hash = DigestUtils.sha256Hex(generatedKey);
    ApiKey keyEntity = ApiKey.builder().hash(hash).owner(owner.toEntity()).build();
    apiKeyRepository.save(keyEntity);
    return new GeneratedApiKey(key, keyEntity);
  }

  public void invalidateKey(String key) {
    try {
      String hash = DigestUtils.sha256Hex(Base64.getDecoder().decode(key));
      apiKeyRepository.deleteById(hash);
    }catch (IllegalArgumentException ignored){
    }
  }
}
