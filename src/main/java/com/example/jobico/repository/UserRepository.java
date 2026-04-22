package com.example.jobico.repository;

import com.example.jobico.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
	
    Optional<User> findByMobile(String mobile);
    boolean existsByMobile(String mobile);
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}
