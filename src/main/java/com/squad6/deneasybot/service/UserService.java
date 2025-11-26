package com.squad6.deneasybot.service;

import com.squad6.deneasybot.exception.DataIntegrityException;
import com.squad6.deneasybot.exception.InvalidCredentialsException;
import com.squad6.deneasybot.exception.ResourceNotFoundException;
import com.squad6.deneasybot.model.Company;
import com.squad6.deneasybot.model.User;
import com.squad6.deneasybot.model.UserDTO;
import com.squad6.deneasybot.model.UserProfile;
import com.squad6.deneasybot.repository.CompanyRepository;
import com.squad6.deneasybot.repository.UserRepository;
import com.squad6.deneasybot.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, CompanyRepository companyRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public User createUser(UserDTO dto) {
        logger.info("Criando novo usuário no banco: {}", dto.getEmail());

        userRepository.findByEmail(dto.getEmail()).ifPresent(existingUser -> {
            throw new DataIntegrityException("O e-mail '" + dto.getEmail() + "' já está cadastrado.");
        });

        Company company = companyRepository.findById(dto.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa com ID " + dto.getCompanyId() + " não encontrada ao criar usuário."));

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setProfile(dto.getProfile());
        user.setSessionToken(dto.getSessionToken());
        user.setCompany(company);

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserDTO getMyProfile(String sessionToken) {
        if (sessionToken == null || !jwtUtil.isTokenValid(sessionToken)) {
            throw new InvalidCredentialsException("Token inválido ou expirado.");
        }

        User user = userRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new InvalidCredentialsException("Token de sessão não associado a nenhum usuário."));

        return new UserDTO(user);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllEmployees(User manager) {
        if (manager.getProfile() != UserProfile.MANAGER) {
            throw new AccessDeniedException("Apenas Gestores (MANAGER) podem listar funcionários.");
        }
        Long companyId = manager.getCompany().getId();
        return userRepository.findAllByCompanyId(companyId).stream()
                .map(UserDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDTO getEmployeeById(Long employeeId, User manager) {
        if (manager.getProfile() != UserProfile.MANAGER) {
            throw new AccessDeniedException("Apenas Gestores (MANAGER) podem ver funcionários.");
        }

        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário com ID " + employeeId + " não encontrado."));

        if (!employee.getCompany().getId().equals(manager.getCompany().getId())) {
            throw new AccessDeniedException("Acesso negado: O funcionário não pertence à sua empresa.");
        }

        return new UserDTO(employee);
    }

    @Transactional
    public void updateUser(Long userIdToUpdate, UserDTO updateData, User authenticatedUser) {
        User userToUpdate = userRepository.findById(userIdToUpdate)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com ID " + userIdToUpdate + " não encontrado."));

        boolean isManagerUpdatingEmployee = authenticatedUser.getProfile() == UserProfile.MANAGER &&
                authenticatedUser.getCompany().getId().equals(userToUpdate.getCompany().getId());

        boolean isUpdatingSelf = authenticatedUser.getId().equals(userIdToUpdate);

        if (!isManagerUpdatingEmployee && !isUpdatingSelf) {
            throw new AccessDeniedException("Você não tem permissão para atualizar este usuário.");
        }

        userToUpdate.setName(updateData.getName());
        userToUpdate.setPhone(updateData.getPhone());

        if (!userToUpdate.getEmail().equals(updateData.getEmail())) {
            userRepository.findByEmail(updateData.getEmail()).ifPresent(existingUser -> {
                throw new DataIntegrityException("O e-mail '" + updateData.getEmail() + "' já está em uso.");
            });
            userToUpdate.setEmail(updateData.getEmail());
        }

        if (isManagerUpdatingEmployee && !isUpdatingSelf && updateData.getProfile() != null) {
            userToUpdate.setProfile(updateData.getProfile());
        }

        User updatedUser = userRepository.save(userToUpdate);
        new UserDTO(updatedUser);
    }

    @Transactional
    public void deleteEmployee(Long employeeId, User manager) {
        if (manager.getProfile() != UserProfile.MANAGER) {
            throw new AccessDeniedException("Apenas Gestores (MANAGER) podem deletar funcionários.");
        }

        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário com ID " + employeeId + " não encontrado."));

        if (!employee.getCompany().getId().equals(manager.getCompany().getId())) {
            throw new AccessDeniedException("Acesso negado: O funcionário não pertence à sua empresa.");
        }

        if (employee.getId().equals(manager.getId())) {
            throw new AccessDeniedException("Um gestor não pode deletar a si mesmo.");
        }

        userRepository.delete(employee);
    }

    @Transactional
    public UserDTO updateManager(Long userId, UserDTO updateData) {
        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Gestor com ID " + userId + " não encontrado."));

        userToUpdate.setName(updateData.getName());
        userToUpdate.setPhone(updateData.getPhone());

        if (!userToUpdate.getEmail().equals(updateData.getEmail())) {
            userRepository.findByEmail(updateData.getEmail()).ifPresent(existing -> {
                throw new DataIntegrityException("O e-mail '" + updateData.getEmail() + "' já está em uso.");
            });
            userToUpdate.setEmail(updateData.getEmail());
        }

        return new UserDTO(userRepository.save(userToUpdate));
    }

    @Transactional
    public void deleteManager(Long userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
        } else {
            throw new ResourceNotFoundException("Gestor não encontrado.");
        }
    }
}