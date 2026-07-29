package com.lastkey.backend.security.service;

import com.lastkey.backend.user.entity.User;
import com.lastkey.backend.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Manual constructor: Lombok problem avoid hoga
    public CustomUserDetailsService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(
            String email
    ) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(
                        () -> new UsernameNotFoundException(
                                "User not found with email: " + email
                        )
                );

        boolean enabled =
                user.getAccountStatus() != null
                        && "ACTIVE".equals(
                        user.getAccountStatus().name()
                );

        boolean accountNonLocked =
                !Boolean.TRUE.equals(
                        user.getAccountLocked()
                );

        String roleName = user.getRole() != null
                && user.getRole().getName() != null
                ? user.getRole().getName().name()
                : "USER";

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),

                // Account is enabled only when status is ACTIVE
                enabled,

                // Account not expired
                true,

                // Credentials not expired
                true,

                // Account is not locked
                accountNonLocked,

                Collections.singletonList(
                        new SimpleGrantedAuthority(
                                "ROLE_" + roleName
                        )
                )
        );
    }
}