package com.example.jobico.security;

import com.example.jobico.entity.Admin;
import com.example.jobico.entity.User;
import com.example.jobico.repository.AdminRepository;
import com.example.jobico.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private AdminRepository adminRepository;

	@Override
	public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
		Optional<Admin> adminOpt = adminRepository.findByEmail(identifier);
		if (adminOpt.isPresent()) {
			Admin admin = adminOpt.get();
			return new org.springframework.security.core.userdetails.User(admin.getEmail(), admin.getPassword(),
					List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
		}
		User user = userRepository.findByMobile(identifier).or(() -> userRepository.findByEmail(identifier))
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));

		String password = (user.getPassword() != null) ? user.getPassword() : "";

		return new org.springframework.security.core.userdetails.User(
				(user.getEmail() != null && "ROLE_ADMIN".equals(user.getRole())) ? user.getEmail() : user.getMobile(),
				password, List.of(new SimpleGrantedAuthority(user.getRole())));
	}
}