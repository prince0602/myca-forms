package com.mynx.myca500.patient_forms.repository;


import com.mynx.myca500.patient_forms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByKeycloakUserId(UUID keycloakUserId);
    Optional<User> findByAccountId(String accountId);

    Optional<User> findByEmail(String email);
}
