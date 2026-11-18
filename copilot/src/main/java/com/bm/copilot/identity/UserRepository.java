package com.bm.copilot.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for users. Spring generates the implementation at startup. */
interface UserRepository extends JpaRepository<User, UUID> {

    /** Usernames are stored in lowercase, so callers must lowercase the name they search for. */
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}