package com.emsi.hospital.service;

import com.emsi.hospital.dto.LoginRequest;
import com.emsi.hospital.dto.LoginResponse;
import com.emsi.hospital.dto.CreateUserRequest;
import com.emsi.hospital.dto.UserResponse;
import com.emsi.hospital.model.AppUser;
import com.emsi.hospital.model.Refrigerator;
import com.emsi.hospital.repository.AppUserRepository;
import com.emsi.hospital.repository.RefrigeratorRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final RefrigeratorRepository refrigeratorRepository;

    public AuthService(AppUserRepository userRepository, RefrigeratorRepository refrigeratorRepository) {
        this.userRepository = userRepository;
        this.refrigeratorRepository = refrigeratorRepository;
    }

    public LoginResponse login(LoginRequest request) {
        AppUser appUser = userRepository.findByEmailIgnoreCase(request.email())
                .filter(user -> user.getPassword().equals(request.password()))
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Email ou mot de passe incorrect"));

        UserResponse user = toResponse(appUser);
        return new LoginResponse(user, createDemoToken(user));
    }

    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(CONFLICT, "Un utilisateur existe deja avec cet email");
        }

        AppUser user = new AppUser();
        user.setEmail(request.email());
        user.setPassword(request.password());
        user.setName(request.name());
        user.setRole(request.role());
        if (request.assignedFridgeIds() != null && !request.assignedFridgeIds().isEmpty()) {
            user.setAssignedFridges(new ArrayList<>(refrigeratorRepository.findByDeviceIdIn(request.assignedFridgeIds())));
        }
        return toResponse(userRepository.save(user));
    }

    public UserResponse updateAssignedFridges(Long userId, List<String> fridgeIds) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Utilisateur introuvable"));
        List<Refrigerator> fridges = fridgeIds == null ? List.of() : refrigeratorRepository.findByDeviceIdIn(fridgeIds);
        user.setAssignedFridges(new ArrayList<>(fridges));
        return toResponse(userRepository.save(user));
    }

    private String createDemoToken(UserResponse user) {
        String payload = """
                {"sub":"%s","email":"%s","role":"%s","exp":%d}
                """.formatted(user.id(), user.email(), user.role(), Instant.now().plusSeconds(3600).toEpochMilli());
        return Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private UserResponse toResponse(AppUser user) {
        List<String> fridgeIds = user.getAssignedFridges().stream()
                .map(Refrigerator::getDeviceId)
                .toList();
        return new UserResponse(
                String.valueOf(user.getId()),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                fridgeIds
        );
    }
}
