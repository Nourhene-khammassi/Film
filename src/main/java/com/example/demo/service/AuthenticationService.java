package com.example.demo.service;

import com.example.demo.config.JwtService;
import com.example.demo.dto.AuthenticationRequest;
import com.example.demo.dto.AuthenticationResponse;
import com.example.demo.dto.RegistrationRequest;
import com.example.demo.entity.Role;
import com.example.demo.entity.Token;
import com.example.demo.entity.TokenType;
import com.example.demo.entity.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.TokenRepository;
import com.example.demo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.var;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    //injection des dépendences
	 private final RoleRepository roleRepository;
  private final UserRepository repository;
  private final TokenRepository tokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;

  public AuthenticationResponse register(RegistrationRequest request) {
	
	  Set<String> strRoles = request.getRoles();
      Set<Role> roles = new HashSet<>();
      //System.err.println(strRoles[]);
      if (strRoles == null) {
          Role userRole = roleRepository.findByName("CLIENT")
                  .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(userRole);
      } else {
          strRoles.forEach(role -> {

                  Role adminRole = roleRepository.findByName(role)
                          .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                  roles.add(adminRole);

          });
      }

	  

	  User  user = User.builder()
        .fullname(request.getFullname())
    
        .email(request.getEmail())
        .password(passwordEncoder.encode(request.getPassword()))
        .build();
	  user.setRoles(roles);
    var savedUser = repository.save(user);
    var jwtToken = jwtService.generateToken(savedUser);
    var refreshToken = jwtService.generateRefreshToken(user);
    saveUserToken(savedUser, jwtToken);
    return AuthenticationResponse.builder()
        .accessToken(jwtToken)
            .refreshToken(refreshToken)
        .build();
  }

  public AuthenticationResponse authenticate(AuthenticationRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getEmail(),
            request.getPassword()
        ));
    var user = repository.findByEmail(request.getEmail())
        .orElseThrow();
    var jwtToken = jwtService.generateToken(user);
    var refreshToken = jwtService.generateRefreshToken(user);
    revokeAllUserTokens(user);
    saveUserToken(user, jwtToken);
    return AuthenticationResponse.builder()
        .accessToken(jwtToken)
            .refreshToken(refreshToken)
        .build();
  }

  private void saveUserToken(User user, String jwtToken) {
    var token = Token.builder()
        .user(user)
        .token(jwtToken)
        .tokenType(TokenType.BEARER)
        .expired(false)
        .revoked(false)
        .build();
    tokenRepository.save(token);
  }

  private void revokeAllUserTokens(User user) {
    var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
    if (validUserTokens.isEmpty())
      return;
    validUserTokens.forEach(token -> {
      token.setExpired(true);
      token.setRevoked(true);
    });
    tokenRepository.saveAll(validUserTokens);
  }

  public void refreshToken(
          HttpServletRequest request,
          HttpServletResponse response
  ) throws IOException {
    final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    final String refreshToken;
    final String userEmail;
    if (authHeader == null ||!authHeader.startsWith("Bearer ")) {
      return;
    }
    refreshToken = authHeader.substring(7);
    userEmail = jwtService.extractUsername(refreshToken);
    if (userEmail != null) {
      var user = this.repository.findByEmail(userEmail)
              .orElseThrow();
      if (jwtService.isTokenValid(refreshToken, user)) {
        var accessToken = jwtService.generateToken(user);
        revokeAllUserTokens(user);
        saveUserToken(user, accessToken);
        var authResponse = AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
        new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
      }
    }
  }
  // les etapes : ajouter dans classe principal  PfaProjectApplication / ajouter abstract classeEntity 
  // methode pour ajouter un admin authomatiquement et l'enregistrer dans la base de donnée-------------- creer un super admin qui peut ajouter aprés des autres admins
  public void createadmine() {
		//create role
		if (!roleRepository.existsByName("ADMIN")) {
			Role roleAdmin = new Role();
			roleAdmin.setName("ADMIN");
			Role dpt = roleRepository.save(roleAdmin);
			

		}
		if (!roleRepository.existsByName("CLIENT")) {
			Role roleAdmin = new Role();
			roleAdmin.setName("CLIENT");
			Role dpt = roleRepository.save(roleAdmin);
			

		}
		User userAdmin = new User();
		User savedUser = null;
		String email = "nourhene@gmail.com";
		if (!repository.existsByEmail(email)) {
			userAdmin.setEmail("nourhene@gmail.com");
			userAdmin.setFullname("nourhene");
			userAdmin.setPassword(new BCryptPasswordEncoder().encode("1234"));
	        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	        Date date= new Date();
	        try {
			 date = dateFormat.parse("1988-08-16");
			 userAdmin.setDateNaissance(date);
		  } catch (ParseException e) {
	            e.printStackTrace();
	        }
			Set<Role> roles = new HashSet<>();
			Role userRole = roleRepository.findByName("ADMIN")
					.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
			roles.add(userRole);
			userAdmin.setRoles(roles);
			
			
			savedUser = repository.save(userAdmin);
		}

	}//--------------------------

}
 