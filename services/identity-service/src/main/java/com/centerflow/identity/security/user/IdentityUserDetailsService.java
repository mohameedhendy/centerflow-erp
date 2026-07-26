package com.centerflow.identity.security.user;

import com.centerflow.identity.authorization.domain.RoleName;
import com.centerflow.identity.authorization.repository.RoleRepository;
import com.centerflow.identity.user.domain.User;
import com.centerflow.identity.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IdentityUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public IdentityUserDetailsService(
            UserRepository userRepository,
            RoleRepository roleRepository
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(
                        () -> new UsernameNotFoundException(
                                "Invalid email or password"
                        )
                );

        List<RoleName> roles =
                roleRepository.findRoleNamesByUserId(
                        user.getId()
                );

        return IdentityUserPrincipal.from(
                user,
                roles
        );
    }
}