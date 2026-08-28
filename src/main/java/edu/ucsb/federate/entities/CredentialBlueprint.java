package edu.ucsb.federate.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.re2j.Pattern;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.hibernate.proxy.HibernateProxy;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Table(indexes = @Index(name = "credential_blueprint_github_organization_idx", columnList = "github_organization"))
public class CredentialBlueprint {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @JoinColumn(name = "creator_id")
  @ManyToOne(fetch = FetchType.LAZY)
  @Exclude
  @JsonIgnore
  private UserEntity creator;

  @Column(name="github_organization")
  private String githubOrganization;

  @ElementCollection
  @CollectionTable(name = "domain_regex",
  joinColumns = @JoinColumn(name = "credential_blueprint_id"))
  @Column(name="domain_regex")
  @Convert(converter = RegexConverter.class)
  @Builder.Default
  private List<Pattern> domains = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "repo_regex",
      joinColumns = @JoinColumn(name = "credential_blueprint_id"))
  @Column(name="repo_regex")
  @Convert(converter = RegexConverter.class)
  @Builder.Default
  private List<Pattern> repos = new ArrayList<>();

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    Class<?> oEffectiveClass = o instanceof HibernateProxy
        ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
        : o.getClass();
    Class<?> thisEffectiveClass = this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer()
        .getPersistentClass() : this.getClass();
    if (thisEffectiveClass != oEffectiveClass) {
      return false;
    }
    CredentialBlueprint that = (CredentialBlueprint) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer()
        .getPersistentClass().hashCode() : getClass().hashCode();
  }
}
