package com.backend.gapfinder.repositories;

import com.backend.gapfinder.models.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserModel, Long> {


    // Finds users whose name contains the text, ignoring case
    List<UserModel> findByNameContainingIgnoreCase(String name);

    // Finds a user by email (used in login and in the JWT filter).
    // Spring Data generates the query from the method name.
    Optional<UserModel> findByEmail(String email);

    // Checks whether an account with this email already exists (used in register)
    boolean existsByEmail(String email);
}