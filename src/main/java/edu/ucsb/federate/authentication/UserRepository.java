package edu.ucsb.federate.authentication;

import edu.ucsb.federate.entities.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

  Optional<UserEntity> findByGoogleSub(String sub);
}
