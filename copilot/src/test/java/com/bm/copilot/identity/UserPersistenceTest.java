package com.bm.copilot.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bm.copilot.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/** Checks the users table, the User entity, the demo seeder and password hashing together. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional // every test rolls back its own changes
class UserPersistenceTest {

    @Autowired
    UserRepository users;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void demoEmployeeExistsWithAHashedPassword() {
        User employee = users.findByUsername("employee1").orElseThrow();

        assertThat(employee.getRole()).isEqualTo(Role.EMPLOYEE);
        assertThat(employee.getPasswordHash()).isNotEqualTo("Employee#123").startsWith("$2");
        assertThat(passwordEncoder.matches("Employee#123", employee.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("wrong-password", employee.getPasswordHash())).isFalse();
    }

    @Test
    void demoSupervisorHasTheSupervisorRole() {
        User supervisor = users.findByUsername("supervisor1").orElseThrow();

        assertThat(supervisor.getRole()).isEqualTo(Role.SUPERVISOR);
        assertThat(supervisor.isEnabled()).isTrue();
    }

    @Test
    void usernameIsStoredTrimmedAndInLowercase() {
        User saved = users.saveAndFlush(new User("  Alice ", "hash", "Alice Example", Role.EMPLOYEE));

        assertThat(saved.getId()).isNotNull();
        assertThat(users.findByUsername("alice")).isPresent();
    }

    @Test
    void duplicateUsernameIsRejectedByTheDatabase() {
        users.saveAndFlush(new User("bob", "hash", "Bob One", Role.EMPLOYEE));

        assertThatThrownBy(() -> users.saveAndFlush(new User("bob", "hash2", "Bob Two", Role.EMPLOYEE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
