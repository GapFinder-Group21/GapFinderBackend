package com.backend.gapfinder.repositories;

import com.backend.gapfinder.models.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserModel, Long> {


    // Finds users whose name contains the text, ignoring case
    List<UserModel> findByNameContainingIgnoreCase(String name);
}