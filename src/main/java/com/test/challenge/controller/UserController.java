package com.test.challenge.controller;

import com.test.challenge.dto.AuthDto.AuthRequestDTO;
import com.test.challenge.dto.AuthDto.AuthResponseDTO;
import com.test.challenge.dto.AuthDto.BatchResponseDTO;
import com.test.challenge.dto.AuthDto.UserDTO;
import com.test.challenge.entities.User;
import com.test.challenge.repositories.UserRepository;
import com.test.challenge.service.impl.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request) {
        return ResponseEntity.ok(userService.authenticate(request));
    }

    @GetMapping("/generate")
    public ResponseEntity<byte[]> generateUsers(@RequestParam int count) {
        System.out.println("ok ");
        String users = userService.generateUsers(count);
        byte[] data;
        try {
            data = users.getBytes("UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        String timestamp = Long.toString(System.currentTimeMillis());
        String filename = "random_users_" + timestamp + ".json";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData(filename, filename);
        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

    @PostMapping(path = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
                                        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BatchResponseDTO> batchImportUsers(@RequestPart("file") MultipartFile file) {
        try {
            BatchResponseDTO response = userService.batchImportUsers(file.getInputStream());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BatchResponseDTO(0, -1));
        }
    }


    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        List<UserDTO> users = userService.getCurrentUser();
        return ResponseEntity.ok(users);
    }



    @GetMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
        UserDTO user = userService.findByUserName(username);
        return ResponseEntity.ok(user);
    }
}
