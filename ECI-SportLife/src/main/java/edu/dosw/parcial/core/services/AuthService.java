package edu.dosw.parcial.core.services;

import edu.dosw.parcial.controller.dtos.request.LoginRequest;
import edu.dosw.parcial.controller.dtos.request.RegisterRequest;
import edu.dosw.parcial.controller.dtos.response.LoginResponse;
import edu.dosw.parcial.controller.dtos.response.RegisterResponse;
import edu.dosw.parcial.core.models.Role;
import edu.dosw.parcial.core.utils.ConflictException;
import edu.dosw.parcial.core.utils.JwtUtil;
import edu.dosw.parcial.persistence.entities.UserEntity;
import edu.dosw.parcial.persistence.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.expiration}")
    private long expirationSeconds;

    // Registra un nuevo usuario validando que el email no esté en uso y cifrando la contraseña
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Ya existe una cuenta registrada con este correo electrónico");
        }

        UserEntity user = UserEntity.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        UserEntity saved = userRepository.save(user);

        return new RegisterResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getCreatedAt());
    }

    // Autentica al usuario y genera un token JWT — siempre responde igual si falla para no revelar si el email existe
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException ex) {
            throw new org.springframework.security.authentication.BadCredentialsException("Credenciales inválidas");
        }

        UserEntity user = userRepository.findByEmail(request.email()).orElseThrow();
        String token = jwtUtil.generateToken(user);

        return new LoginResponse(token, "Bearer", expirationSeconds);
    }
}
