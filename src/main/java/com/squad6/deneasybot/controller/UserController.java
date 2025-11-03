package com.squad6.deneasybot.controller;

import com.squad6.deneasybot.model.UserDTO;
import com.squad6.deneasybot.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getMyProfile(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "").trim();

        UserDTO userDTO = userService.getMyProfile(token);

        return ResponseEntity.ok(userDTO);
    }
}