package com.test.challenge.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.test.challenge.dto.AuthDto.AuthRequestDTO;
import com.test.challenge.dto.AuthDto.AuthResponseDTO;
import com.test.challenge.dto.AuthDto.BatchResponseDTO;
import com.test.challenge.dto.AuthDto.UserDTO;
import com.test.challenge.entities.Token;
import com.test.challenge.entities.User;
import com.test.challenge.enums.Role;
import com.test.challenge.repositories.TokenRepository;
import com.test.challenge.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
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
    private final ObjectMapper objectMapper;



    @Autowired
    private ModelMapper modelMapper;

    public UserService(UserRepository repository,
                                 PasswordEncoder passwordEncoder,
                                 JwtService jwtService,
                                 TokenRepository tokenRepository,
                                 AuthenticationManager authenticationManager,
                                 ObjectMapper objectMapper) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenRepository = tokenRepository;
        this.authenticationManager = authenticationManager;
        this.objectMapper = objectMapper;
        this.faker = new Faker();
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

    public String generateUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User user = new User();
            user.setFirstName(faker.name().firstName());
            user.setLastName(faker.name().lastName());
            user.setBirthDate(faker.date().birthday());
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
        try {
            return objectMapper.writeValueAsString(users);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }


    public BatchResponseDTO batchImportUsers(InputStream inputStream) {
        TypeReference<List<User>> typeReference = new TypeReference<List<User>>() {};

        try {
            List<User> users = objectMapper.readValue(inputStream, typeReference);
            int successfullyInserted = 0;
            int failedToInsert = 0;

            for (User user : users) {
                try {
                    repository.save(user);
                    successfullyInserted++;
                } catch (Exception e) {
                    failedToInsert++;
                    e.printStackTrace();
                }
            }
            return new BatchResponseDTO(successfullyInserted, failedToInsert);
        } catch (IOException e) {
            e.printStackTrace();
            return new BatchResponseDTO(0, -1);
        }




    }



    public List<UserDTO> getCurrentUser(){
        return Arrays.asList(modelMapper.map(repository.findUsersLoggedOut(), UserDTO[].class));

    }

    public UserDTO findByUserName(String username){
        User user = repository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return modelMapper.map(user, UserDTO.class);
    }

    private String generateRandomPassword() {
        int length = faker.random().nextInt(5) + 6;
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
