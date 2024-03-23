package com.test.challenge.service.impl;

import com.github.javafaker.Faker;
import com.test.challenge.dto.AuthDto.AuthRequestDTO;
import com.test.challenge.dto.AuthDto.AuthResponseDTO;
import com.test.challenge.dto.AuthDto.UserDTO;
import com.test.challenge.entities.Token;
import com.test.challenge.entities.User;
import com.test.challenge.enums.Role;
import com.test.challenge.repositories.TokenRepository;
import com.test.challenge.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private final TokenRepository tokenRepository;

    private final AuthenticationManager authenticationManager;

    private final Faker faker;



    @Autowired
    private ModelMapper modelMapper;

    public UserService(UserRepository repository,
                                 PasswordEncoder passwordEncoder,
                                 JwtService jwtService,
                                 TokenRepository tokenRepository,
                                 AuthenticationManager authenticationManager) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenRepository = tokenRepository;
        this.authenticationManager = authenticationManager;
        this.faker = new Faker();
    }

    public AuthResponseDTO register(AuthRequestDTO request) {

        // check if user already exist. if exist than authenticate the user
        if(repository.findByUsername(request.getUsername()).isPresent()) {
            return new AuthResponseDTO("User already exist");
        }
        try {
            var user = User.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .birthDate(request.getBirthDate())
                    .city(request.getCity())
                    .country(request.getCountry())
                    .avatar(request.getAvatar())
                    .company(request.getCompany())
                    .jobPosition(request.getJobPosition())
                    .mobile(request.getMobile())
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(Role.valueOf(request.getRole()))
                    .build();


            user = repository.save(user);

            String jwt = jwtService.generateToken(user);
            saveUserToken(jwt, user);

            return AuthResponseDTO.builder()
                    .token(jwt)
                    .build();

        } catch (DataIntegrityViolationException e) {
            return new AuthResponseDTO(e.getMessage());
        }

    }

    public AuthResponseDTO authenticate(AuthRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = repository.findByUsername(request.getUsername()).orElseThrow();
        String jwt = jwtService.generateToken(user);


        revokeAllTokenByUser(user);
        saveUserToken(jwt, user);

        return AuthResponseDTO.builder()
                .token(jwt)
                .build();

    }

    public List<User> generateUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User user = new User();
            user.setFirstName(faker.name().firstName());
            user.setLastName(faker.name().lastName());
            user.setBirthDate(faker.date().birthday().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            user.setCity(faker.address().city());
            user.setCountry(faker.address().countryCode());
            user.setAvatar(faker.internet().avatar());
            user.setCompany(faker.company().name());
            user.setJobPosition(faker.job().title());
            user.setMobile(faker.phoneNumber().cellPhone());
            user.setUsername(faker.internet().emailAddress());
            user.setEmail(faker.internet().emailAddress());
            user.setPassword(generateRandomPassword());
            user.setRole(faker.random().nextBoolean() ? Role.USER : Role.ADMIN);
            users.add(user);
        }
        return users;
    }


    public Map<String, Integer> batchImportUsers(MultipartFile file) {
        Map<String, Integer> result = new HashMap<>();
        int totalCount = 0;
        int importedCount = 0;
        int failedCount = 0;

        try {
            List<User> users = Arrays.stream(new String(file.getBytes(), StandardCharsets.UTF_8).split("\n"))
                    .map(this::parseUser)
                    .collect(Collectors.toList());

            totalCount = users.size();

            for (User user : users) {
                if (!repository.existsByUsername(user.getUsername())) {
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                    repository.save(user);
                    importedCount++;
                } else {
                    failedCount++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        result.put("totalCount", totalCount);
        result.put("importedCount", importedCount);
        result.put("failedCount", failedCount);

        return result;
    }


    private User parseUser(String line) {
        String[] fields = line.replaceAll("\\[|\\]|\"", "").split(",");
        User user = new User();
        user.setFirstName(fields[0].trim());
        user.setLastName(fields[1].trim());
        user.setBirthDate(LocalDate.parse(fields[2].trim()));
        user.setCity(fields[3].trim());
        user.setCountry(fields[4].trim());
        user.setAvatar(fields[5].trim());
        user.setCompany(fields[6].trim());
        user.setJobPosition(fields[7].trim());
        user.setMobile(fields[8].trim());
        user.setUsername(fields[9].trim());
        user.setEmail(fields[10].trim());
        user.setPassword(fields[11].trim());
        user.setRole(fields[12].trim().equalsIgnoreCase("admin") ? Role.ADMIN : Role.USER);
        return user;
    }

    public List<UserDTO> getCurrentUser(){
        return Arrays.asList(modelMapper.map(repository.findUsersLoggedOut(), UserDTO[].class));

    }

    private String generateRandomPassword() {
        int length = faker.random().nextInt(5) + 6; // Longueur entre 6 et 10 caractères
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = faker.random().nextInt(characters.length());
            sb.append(characters.charAt(index));
        }
        return sb.toString();
    }

    private void revokeAllTokenByUser(User user) {
        List<Token> validTokens = tokenRepository.findAllTokensByUser(user.getId());
        if(validTokens.isEmpty()) {
            return;
        }

        validTokens.forEach(t-> {
            t.setLoggedOut(true);
        });

        tokenRepository.saveAll(validTokens);
    }
    private void saveUserToken(String jwt, User user) {
        Token token = new Token();
        token.setToken(jwt);
        token.setLoggedOut(false);
        token.setUser(user);
        tokenRepository.save(token);
    }

}
