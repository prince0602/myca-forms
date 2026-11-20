package com.mynx.myca500.patient_forms.repository;

import com.mynx.myca500.patient_forms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByKeycloakUserId(UUID keycloakUserId);
}
