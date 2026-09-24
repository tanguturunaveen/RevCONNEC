package org.revature.revconnect.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_whenFoundByUsername_returnsUserDetails() {
        User user = user(1L, "john", "john@test.com");
        when(userRepository.findByUsernameOrEmail("john", "john")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("john");

        assertEquals("john", details.getUsername());
    }

    @Test
    void loadUserByUsername_whenFoundByEmail_returnsUserDetails() {
        User user = user(2L, "jane", "jane@test.com");
        when(userRepository.findByUsernameOrEmail("jane@test.com", "jane@test.com")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("jane@test.com");

        assertEquals("jane", details.getUsername());
    }

    @Test
    void loadUserByUsername_whenMissing_throwsUsernameNotFound() {
        when(userRepository.findByUsernameOrEmail("missing", "missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("missing"));
    }

    @Test
    void loadUserById_whenFound_returnsUserDetails() {
        User user = user(3L, "alex", "alex@test.com");
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserById(3L);

        assertEquals("alex", details.getUsername());
    }

    @Test
    void loadUserById_whenMissing_throwsUsernameNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserById(99L));
    }

    private User user(Long id, String username, String email) {
        return User.builder()
                .id(id)
                .username(username)
                .email(email)
                .name(username)
                .password("pwd")
                .privacy(Privacy.PUBLIC)
                .userType(UserType.PERSONAL)
                .build();
    }
}
