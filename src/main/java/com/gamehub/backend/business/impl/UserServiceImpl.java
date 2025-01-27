package com.gamehub.backend.business.impl;

import com.gamehub.backend.configuration.exception.UserNotFoundException;
import com.gamehub.backend.domain.FriendRelationship;
import com.gamehub.backend.dto.FriendRequestDTO;
import com.gamehub.backend.dto.UserDTO;
import com.gamehub.backend.business.UserService;
import com.gamehub.backend.configuration.security.token.JwtUtil;
import com.gamehub.backend.domain.Role;
import com.gamehub.backend.domain.User;
import com.gamehub.backend.persistence.FriendRelationshipRepository;
import com.gamehub.backend.persistence.UserRepository;
import com.gamehub.backend.persistence.mapper.UserMapper;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final FriendRelationshipRepository friendRelationshipRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final MailService mailService;
    @Autowired
    public UserServiceImpl(UserRepository userRepository, FriendRelationshipRepository friendRelationshipRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder, UserMapper userMapper, MailService mailService) {
        this.userRepository = userRepository;
        this.friendRelationshipRepository = friendRelationshipRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.mailService = mailService;
    }

    @Override
    public UserDTO createUser(UserDTO userDTO) {
        validateUserDTO(userDTO);
        User user = prepareUserEntity(userDTO);
        user = userRepository.save(user);
        return buildUserDTOwithJwt(user);
    }

    @Override
    public Optional<UserDTO> getUserById(Long id) {
        return Optional.ofNullable(userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id " + id)));
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    existingUser.setUsername(userDTO.getUsername());
                    existingUser.setEmail(userDTO.getEmail());
                    if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty() &&
                            !passwordEncoder.matches(userDTO.getPassword(), existingUser.getPasswordHash())) {
                        existingUser.setPasswordHash(passwordEncoder.encode(userDTO.getPassword()));
                    }
                    existingUser.setDescription(userDTO.getDescription());
                    if (userDTO.getRole() != null && !userDTO.getRole().isEmpty()) {
                        existingUser.setRoles(userDTO.getRole().stream().map(Role::valueOf).toList());
                    }
                    userRepository.save(existingUser);
                    return mapToDto(existingUser);
                }).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Override
    public void updateUserProfilePicture(Long userId, String fileName) {
        userRepository.findById(userId)
                .map(user -> {
                    user.setProfilePicture(fileName);
                    return userRepository.save(user);
                }).orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    private User prepareUserEntity(UserDTO userDTO) {
        User user = userMapper.toEntity(userDTO);
        if (userDTO.getId() == null) {
            user.setRoles(Collections.singletonList(Role.USER));
        }
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(userDTO.getPassword()));
        }
        return user;
    }

    private UserDTO buildUserDTOwithJwt(User user) {
        UserDTO dto = userMapper.toDto(user);
        dto.setJwt(jwtUtil.generateToken(dto.getUsername()));
        return dto;
    }

    @Override
    public Optional<UserDTO> login(UserDTO userDTO) {
        return userRepository.findByUsername(userDTO.getUsername())
                .filter(user -> passwordEncoder.matches(userDTO.getPassword(), user.getPasswordHash()))
                .map(this::mapToDto);
    }

    private UserDTO mapToDto(User user) {
        UserDTO dto = userMapper.toDto(user);
        dto.setJwt(jwtUtil.generateToken(user.getUsername()));
        return dto;
    }

    @Override
    public FriendRelationship sendRequest(Long userId, Long friendId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found with id " + userId));
        User friend = userRepository.findById(friendId).orElseThrow(() -> new EntityNotFoundException("Friend not found with id " + friendId));

        boolean friendRequestExists = friendRelationshipRepository.existsByUserAndFriend(user, friend) ||
                friendRelationshipRepository.existsByUserAndFriend(friend, user);
        boolean pendingRequestExists = friendRelationshipRepository.existsByUserAndFriendAndStatus(user, friend, FriendRelationship.Status.PENDING) ||
                friendRelationshipRepository.existsByUserAndFriendAndStatus(friend, user, FriendRelationship.Status.PENDING);

        if (friendRequestExists || pendingRequestExists) {
            throw new IllegalArgumentException("Friend request already sent or user is already your friend");
        }

        FriendRelationship relationship = new FriendRelationship();
        relationship.setUser(user);
        relationship.setFriend(friend);
        relationship.setStatus(FriendRelationship.Status.PENDING);
        return friendRelationshipRepository.save(relationship);
    }

    @Override
    public List<FriendRequestDTO> getPendingRequests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id " + userId));

        List<FriendRelationship> pendingRelationships = friendRelationshipRepository.findByFriendAndStatus(user, FriendRelationship.Status.PENDING);

        List<FriendRequestDTO> pendingRequests = pendingRelationships.stream()
                .map(relationship -> new FriendRequestDTO(
                        relationship.getId(),
                        userMapper.toDto(relationship.getUser()),
                        userMapper.toDto(relationship.getFriend()),
                        relationship.getStatus()))
                .toList();

        return pendingRequests;
    }

    @Override
    public FriendRelationship respondToRequest(Long relationshipId, FriendRelationship.Status status) {
        FriendRelationship relationship = friendRelationshipRepository.findById(relationshipId).orElseThrow(() -> new EntityNotFoundException("Friend relationship not found with id " + relationshipId));
        relationship.setStatus(status);
        if (status == FriendRelationship.Status.ACCEPTED) {
            relationship = friendRelationshipRepository.save(relationship);
        }
        return relationship;
    }

    @Override
    public List<FriendRequestDTO> getFriends(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found with id " + userId));
        List<FriendRelationship> friendRelationships = friendRelationshipRepository.findByUserAndStatusOrFriendAndStatus(user, FriendRelationship.Status.ACCEPTED, user, FriendRelationship.Status.ACCEPTED);

        return friendRelationships.stream()
                .map(relationship -> new FriendRequestDTO(
                        relationship.getId(),
                        userMapper.toDto(relationship.getUser()),
                        userMapper.toDto(relationship.getFriend()),
                        relationship.getStatus()))
                .toList();
    }

    @Override
    public void removeFriend(Long relationshipId) {
        FriendRelationship relationship = friendRelationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new EntityNotFoundException("Friend relationship not found with id " + relationshipId));
        friendRelationshipRepository.delete(relationship);
    }

    private void validateUserDTO(UserDTO userDTO) {
        if (!StringUtils.hasText(userDTO.getUsername()) || !StringUtils.hasText(userDTO.getEmail()) || !StringUtils.hasText(userDTO.getPassword())) {
            throw new IllegalArgumentException("Username, email, and password must not be empty");
        }
        if (userRepository.existsByUsername(userDTO.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userDTO.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (!userDTO.getPassword().matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#\\$%\\^&\\*])(?=\\S+$).{8,}$")) {
            throw new IllegalArgumentException("Password must contain at least one digit, one lowercase letter, one uppercase letter, and one special character");
        }
    }

    @Override
    public boolean verifyUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setTokenExpiryDate(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        String resetUrl = "http://localhost:5173/forgot-password?token=" + token;
        String subject = "Password Reset Request";
        String text = "<p>Hello, you received this email because you requested a password change for your account in the GameHub community.</p>"
                + "<p>To reset your password, click the following link: <a href=\"" + resetUrl + "\">Reset password</a></p>"
                + "<p>This reset request will expire in 10 minutes. If it expired, you can request a new one.</p>"
                + "<p>If this wasn't requested by you, you can ignore this email.</p>";

        try {
            mailService.sendMail(user.getEmail(), subject, text);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public boolean validateResetToken(String token) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Invalid token"));

        if (user.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        return true;
    }

    @Override
    public boolean resetPasswordWithToken(String token, String newPassword) {
        if (!validateResetToken(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Invalid or expired token"));

        validateNewPassword(newPassword);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setTokenExpiryDate(null);
        userRepository.save(user);
        return true;
    }

    private void validateNewPassword(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (!password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#\\$%\\^&\\*])(?=\\S+$).{8,}$")) {
            throw new IllegalArgumentException("Password must contain at least one digit, one lowercase letter, one uppercase letter, and one special character");
        }
    }

}
