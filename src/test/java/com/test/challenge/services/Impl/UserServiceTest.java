package com.test.challenge.services.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.challenge.dto.AuthDto.AuthRequestDTO;
import com.test.challenge.dto.AuthDto.AuthResponseDTO;
import com.test.challenge.dto.AuthDto.BatchResponseDTO;
import com.test.challenge.dto.AuthDto.UserDTO;
import com.test.challenge.entities.Token;
import com.test.challenge.entities.User;
import com.test.challenge.enums.Role;
import com.test.challenge.repositories.TokenRepository;
import com.test.challenge.repositories.UserRepository;
import com.test.challenge.service.impl.JwtService;
import com.test.challenge.service.impl.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private ModelMapper modelMapper;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UserService userService;
    private User user;
    private UserDTO userDTO;



    private AuthRequestDTO authRequestDTO;


    @BeforeEach
    public void setUp() {
        user = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .birthDate(new Date())
                .city("New York")
                .country("USA")
                .avatar("avatar_url")
                .company("ABC Company")
                .jobPosition("Software Engineer")
                .mobile("1234567890")
                .username("john_doe")
                .email("john.doe@example.com")
                .password("password123")
                .role(Role.USER)
                .build();
        userDTO = UserDTO.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Smith")
                .birthDate(new Date())
                .city("Los Angeles")
                .country("USA")
                .avatar("avatar_url")
                .company("XYZ Corp.")
                .jobPosition("Data Scientist")
                .mobile("9876543210")
                .username("jane_smith")
                .email("jane.smith@example.com")
                .password("password456")
                .role(Role.ADMIN.name())
                .build();
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        given(securityContext.getAuthentication()).willReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }




    @Test
    public void testAuthenticate() {
        user = User.builder()
                .username("john_doe")
                .password("$2a$10$GZ9bzNmrte3HXXvbHyOnoOC.NL7H4VLco9rOyBI2e1YJ22eQkL15u")
                .role(Role.USER)
                .build();
        given(userRepository.findByUsername("john_doe")).willReturn(java.util.Optional.of(user));
        given(passwordEncoder.matches("password123", user.getPassword())).willReturn(true);
        given(jwtService.generateToken(user)).willReturn("sample_token");
        AuthRequestDTO authRequest = new AuthRequestDTO("john_doe", "password123");
    }



    @Test
    public void testBatchImportUsers() throws Exception {
        String jsonData = "[{\"username\":\"user1\", \"password\":\"pass1\"}, {\"username\":\"user2\", \"password\":\"pass2\"}]";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonData.getBytes());
        List<User> users = Arrays.asList(
                User.builder().username("user1").password("pass1").build(),
                User.builder().username("user2").password("pass2").build()
        );
        when(objectMapper.readValue(any(InputStream.class), any(TypeReference.class))).thenReturn(users);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BatchResponseDTO response = userService.batchImportUsers(inputStream);
        assertEquals(2, response.getSuccessfullyInsertedRows());
        assertEquals(0, response.getFailedToInsertRows());
        verify(userRepository, times(2)).save(any(User.class));
    }



}


