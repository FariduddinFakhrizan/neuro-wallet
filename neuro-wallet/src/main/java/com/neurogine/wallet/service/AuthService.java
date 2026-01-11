package com.neurogine.wallet.service;

import com.neurogine.wallet.entity.User;
import com.neurogine.wallet.entity.Wallet;
import com.neurogine.wallet.exception.BadRequestException;
import com.neurogine.wallet.exception.UnauthorizedException;
import com.neurogine.wallet.repository.UserRepository;
import com.neurogine.wallet.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service
 * Handles user registration and login with BCrypt password encryption.
 * Demonstrates proper security practices and transaction management.
 */
@Service
public class AuthService {
    @Autowired
    private UserRepository userRepo;

    @Autowired
    private WalletRepository walletRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Register a new user with encrypted password
     * 
     * @param username User's username
     * @param password User's plain text password
     * @return Saved user entity
     */
    @Transactional
    public User registerUser(String username, String password) {
        // Check if username already exists
        if (userRepo.findByUsername(username).isPresent()) {
            throw new BadRequestException("Username already exists");
        }

        // Validate input
        if (username == null || username.trim().length() < 3) {
            throw new BadRequestException("Username must be at least 3 characters");
        }
        if (password == null || password.length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters");
        }

        // Create new user with encrypted password
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password)); // BCrypt encryption
        User savedUser = userRepo.save(newUser);

        // Create a wallet for the new user with starting balance
        Wallet wallet = new Wallet();
        wallet.setUserId(savedUser.getId());
        wallet.setBalance(1000.0); // Starting balance of 1000 RM
        walletRepo.save(wallet);

        return savedUser;
    }

    /**
     * Authenticate user and verify password
     * 
     * @param username User's username
     * @param password User's plain text password
     * @return Authenticated user entity
     */
    public User login(String username, String password) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        // Verify password using BCrypt
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        return user;
    }
}
