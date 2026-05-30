package com.drivex;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public String register(@RequestBody User user) {

        User existingUser =
                userRepository.findByUsername(user.getUsername());

        if (existingUser != null) {
            return "Username already exists";
        }

        userRepository.save(user);

        return "User saved in MongoDB";
    }

    @PostMapping("/login")
    public String login(@RequestBody User user) {

        User existingUser =
                userRepository.findByUsername(user.getUsername());

        if (existingUser == null) {
            return "User not found";
        }

        if (!existingUser.getPassword()
                .equals(user.getPassword())) {

            return "Invalid credentials";
        }

          return existingUser.getUsername() + "|/dashboard.html";
    }
}