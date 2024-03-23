package com.test.challenge.controller;

import com.test.challenge.dto.AuthDto.UserDTO;
import com.test.challenge.entities.User;
import com.test.challenge.repositories.UserRepository;
import com.test.challenge.service.impl.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/generate")
    public ResponseEntity<?> generateUsers(@RequestParam int count) {
        List<User> users = userService.generateUsers(count);
        return ResponseEntity.ok(users);
    }

    @PostMapping("/batch")
    public ResponseEntity<?> batchImportUsers(@RequestParam("file") MultipartFile file) {
        Map<String, Integer> result = userService.batchImportUsers(file);
        return ResponseEntity.ok(result);
    }


    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        List<UserDTO> users = userService.getCurrentUser();
        return ResponseEntity.ok(users);
    }



    @GetMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(user);
    }
}
