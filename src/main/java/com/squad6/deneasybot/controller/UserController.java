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
        List<UserDTO> employees = userService.getAllEmployees(manager);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/employee/{id}")
    public ResponseEntity<UserDTO> getEmployeeById(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        User manager = getAuthenticatedUser(authHeader);
        UserDTO employee = userService.getEmployeeById(id, manager);
        return ResponseEntity.ok(employee);
    }

    @PutMapping("/employee/{id}")
    public ResponseEntity<UserDTO> updateEmployee(@PathVariable Long id, @RequestBody UserDTO employeeDto, @RequestHeader("Authorization") String authHeader) {
        User manager = getAuthenticatedUser(authHeader);
        UserDTO updatedEmployee = userService.updateUser(id, employeeDto, manager);
        return ResponseEntity.ok(updatedEmployee);
    }

    @DeleteMapping("/employee/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        User manager = getAuthenticatedUser(authHeader);
        userService.deleteEmployee(id, manager);
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