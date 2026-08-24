package edu.ucsb.federate.authentication;

public interface User {


  public Long getId();
  public String getEmail();
  public String getGoogleSub();
  public String getPictureUrl();
  public String getFullName();
  public String getGivenName();
  public String getFamilyName();

}
