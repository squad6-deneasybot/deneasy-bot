package com.squad6.deneasybot.controller;

import com.squad6.deneasybot.exception.InvalidCredentialsException;
import com.squad6.deneasybot.model.User;
import com.squad6.deneasybot.model.UserDTO;
import com.squad6.deneasybot.model.UserProfile;
import com.squad6.deneasybot.service.AuthService;
import com.squad6.deneasybot.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getMyProfile(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        UserDTO userProfile = userService.getMyProfile(token);
        return ResponseEntity.ok(userProfile);
    }

    @PostMapping("/employee")
    public ResponseEntity<UserDTO> createEmployee(@RequestBody UserDTO employeeDto, @RequestHeader("Authorization") String authHeader) {
        User manager = getAuthenticatedUser(authHeader);
        employeeDto.setProfile(UserProfile.EMPLOYEE);
        employeeDto.setCompanyId(manager.getCompany().getId());
        User createdUser = userService.createUser(employeeDto);
        return new ResponseEntity<>(new UserDTO(createdUser), HttpStatus.CREATED);
    }

    @GetMapping("/employees")
    public ResponseEntity<List<UserDTO>> getAllEmployees(@RequestHeader("Authorization") String authHeader) {
        User manager = getAuthenticatedUser(authHeader);
        return ResponseEntity.ok(userService.getAllEmployees(manager));
    }

    @PostMapping("/manager")
    public ResponseEntity<UserDTO> createManager(@RequestBody UserDTO managerDto) {
        managerDto.setProfile(UserProfile.MANAGER);
        User createdUser = userService.createUser(managerDto);
        return new ResponseEntity<>(new UserDTO(createdUser), HttpStatus.CREATED);
    }

    @PutMapping("/manager/{id}")
    public ResponseEntity<UserDTO> updateManager(@PathVariable Long id, @RequestBody UserDTO managerDto) {
        UserDTO updatedManager = userService.updateManager(id, managerDto);
        return ResponseEntity.ok(updatedManager);
    }

    @DeleteMapping("/manager/{id}")
    public ResponseEntity<Void> deleteManager(@PathVariable Long id) {
        userService.deleteManager(id);
        return ResponseEntity.noContent().build();
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new InvalidCredentialsException("Cabeçalho de autorização ausente ou mal formatado.");
        }
        return authHeader.substring(7);
    }

    private User getAuthenticatedUser(String authHeader) {
        String token = extractToken(authHeader);
        return authService.findUserByToken(token);
    }
}