package com.schoolerp.modules.auth.repository;

import com.schoolerp.modules.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findBySchoolIdAndUsernameAndActiveTrue(UUID schoolId, String username);

    Optional<User> findBySchoolIdAndUsername(UUID schoolId, String username);

    boolean existsBySchoolIdAndUsername(UUID schoolId, String username);
}
