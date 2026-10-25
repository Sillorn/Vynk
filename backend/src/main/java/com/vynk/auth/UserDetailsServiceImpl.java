package com.vynk.auth;

import com.vynk.identity.UserAccount;
import com.vynk.identity.UserAccountRepository;
import com.vynk.identity.UserAccountStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public UserDetailsServiceImpl(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = userAccountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (account.getStatus() != UserAccountStatus.ACTIVE) {
            throw new UsernameNotFoundException("User not active");
        }
        return new User(account.getUsername(), account.getPasswordHash(), List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
