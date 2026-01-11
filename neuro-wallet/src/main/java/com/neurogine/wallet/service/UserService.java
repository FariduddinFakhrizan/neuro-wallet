package com.neurogine.wallet.service;

import com.neurogine.wallet.dto.UpdateUserRequest;
import com.neurogine.wallet.entity.User;
import com.neurogine.wallet.entity.Wallet;
import com.neurogine.wallet.exception.ResourceNotFoundException;
import com.neurogine.wallet.repository.UserRepository;
import com.neurogine.wallet.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * User Service
 * Handles user CRUD operations, profile management, and avatar upload.
 * Demonstrates service layer best practices and transaction management.
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Get all users with pagination
     */
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Get user by ID
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    /**
     * Update user details
     */
    @Transactional
    public User updateUser(Long id, UpdateUserRequest request) {
        User user = getUserById(id);

        if (request.getUsername() != null && !request.getUsername().isEmpty()) {
            // Check if new username is already taken
            userRepository.findByUsername(request.getUsername()).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(id)) {
                    throw new RuntimeException("Username already exists");
                }
            });
            user.setUsername(request.getUsername());
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return userRepository.save(user);
    }

    /**
     * Delete user and associated wallet
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);

        // Delete user's wallet
        walletRepository.findByUserId(id).ifPresent(wallet -> {
            walletRepository.delete(wallet);
        });

        // Delete user's avatar if exists
        if (user.getAvatarUrl() != null) {
            String filename = user.getAvatarUrl().substring(user.getAvatarUrl().lastIndexOf("/") + 1);
            fileStorageService.deleteFile(filename);
        }

        userRepository.delete(user);
    }

    /**
     * Upload user avatar
     */
    @Transactional
    public User uploadAvatar(Long id, MultipartFile file) {
        User user = getUserById(id);

        // Delete old avatar if exists
        if (user.getAvatarUrl() != null) {
            String oldFilename = user.getAvatarUrl().substring(user.getAvatarUrl().lastIndexOf("/") + 1);
            fileStorageService.deleteFile(oldFilename);
        }

        // Store new avatar
        String avatarUrl = fileStorageService.storeFile(file);
        user.setAvatarUrl(avatarUrl);

        return userRepository.save(user);
    }
}
