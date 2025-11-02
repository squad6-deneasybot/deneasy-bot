package com.squad6.deneasybot.controller;

import com.squad6.deneasybot.model.UserMeResponseDTO;
import com.squad6.deneasybot.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserMeResponseDTO> getAuthenticatedUserProfile(Authentication authentication) {
        UserMeResponseDTO userDTO = userService.getAuthenticatedUser(authentication);

        return ResponseEntity.ok(userDTO);
    }
}