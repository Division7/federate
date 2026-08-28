package edu.ucsb.federate.authentication;

import edu.ucsb.federate.entities.UserEntity;
import org.springframework.security.acls.domain.PrincipalSid;

public interface User {


  public Long getId();
  public String getEmail();
  public String getGoogleSub();
  public String getPictureUrl();
  public String getFullName();
  public String getGivenName();
  public String getFamilyName();
  public UserEntity toEntity();
  public PrincipalSid getSid();
  public boolean isAdmin();
  public boolean isManager();
}
