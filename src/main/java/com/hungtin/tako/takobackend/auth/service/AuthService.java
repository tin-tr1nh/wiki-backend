package com.hungtin.tako.takobackend.auth.service;

import com.hungtin.tako.takobackend.auth.http.LoginRequest;
import com.hungtin.tako.takobackend.auth.http.LoginResponse;
import com.hungtin.tako.takobackend.auth.model.UserAccount;
import com.hungtin.tako.takobackend.auth.model.UserToken;
import com.hungtin.tako.takobackend.auth.model.VerifiedToken;
import com.hungtin.tako.takobackend.auth.repo.UserAccountRepo;
import com.hungtin.tako.takobackend.auth.repo.UserTokenRepo;
import com.hungtin.tako.takobackend.auth.repo.VerifiedTokenRepo;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AuthService {

  private final UserAccountRepo userAccountRepo;
  private final VerifiedTokenRepo verifiedTokenRepo;
  private final AuthenticationManager authenticationManager;
  private final UserDetailsService userDetailsService;
  private final UserTokenRepo userTokenRepo;
  private final JwtTokenService jwtTokenService;

  public VerifiedToken makeVerifiedToken(UserAccount userAccount) {
    // create verified token
    String tokenValue = UUID.randomUUID().toString();
    VerifiedToken token = VerifiedToken.builder()
        .user(userAccount)
        .value(tokenValue)
        .expireAt(Instant.now().plus(Duration.ofHours(24)))
        .build();
    verifiedTokenRepo.save(token);

    return token;
  }

  @Transactional
  public boolean verifyToken(String token) {
    Optional<VerifiedToken> verifiedToken = verifiedTokenRepo.findByValue(token);
    if (verifiedToken.isEmpty()) {
      return false;
    }

    UserAccount user = verifiedToken.get().getUser();
    if (Objects.isNull(user)) {
      return false;
    }
    user.setEnable(true);
    userAccountRepo.save(user);
    verifiedTokenRepo.deleteById(verifiedToken.get().getId());

    return true;
  }

  public LoginResponse loginWithJwtToken(LoginRequest request) {
    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
        request.getUsername(), request.getPassword());

    authenticationManager.authenticate(token);
    UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
    String jwtToken = jwtTokenService.generateToken(userDetails);
    // return the response
    return new LoginResponse(jwtToken);
  }

  public LoginResponse loginWithTokenServerBase(LoginRequest request) {
    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
        request.getUsername(),
        request.getPassword());
    // if authenticate without any exception, create the token
    UserAccount user = (UserAccount) userDetailsService.loadUserByUsername(request.getUsername());
    String tokenValue = UUID.randomUUID().toString();
    UserToken userToken = UserToken.builder().userAccount(user).value(tokenValue).build();
    userTokenRepo.save(userToken);

    // return the response
    return new LoginResponse(tokenValue);
  }
}