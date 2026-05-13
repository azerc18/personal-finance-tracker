package com.example.finance.security;

import com.example.finance.entity.Authority;
import com.example.finance.entity.Role;
import com.example.finance.entity.User;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        //Load user từ database
        User user = userRepo.findByEmail(email)
                .orElseThrow(()-> new ResourceNotFoundException("Email is not found"));

        Set<GrantedAuthority> granted = new HashSet<>();

        //Lấy Role từ user
        if(user.getRoles() != null){
            for(Role role: user.getRoles()){
                if(role.getName() != null){
                    granted.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                }

                //Lấy Authority từ Role
                if(role.getAuthorities() != null){
                    for(Authority a: role.getAuthorities()){
                        granted.add(new SimpleGrantedAuthority(a.getName()));
                    }
                }
            }
        }

        //Tạo User Details
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                granted
        );
    }
}
