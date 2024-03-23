package com.test.challenge.repositories;


import com.test.challenge.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameOrEmail(String username, String email);
    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    @Query("SELECT u FROM User u JOIN Token t ON u.id = t.user.id WHERE t.loggedOut = true")
    List<User> findUsersLoggedOut();
}
