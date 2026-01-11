package com.neurogine.wallet.service;

import com.neurogine.wallet.entity.Contact;
import com.neurogine.wallet.entity.User;
import com.neurogine.wallet.exception.BadRequestException;
import com.neurogine.wallet.exception.ResourceNotFoundException;
import com.neurogine.wallet.repository.ContactRepository;
import com.neurogine.wallet.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Contact Service
 * Manages address book contacts
 */
@Service
public class ContactService {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Add a contact
     */
    @Transactional
    public Contact addContact(Long userId, Long contactUserId, String nickname) {
        // Validate contact user exists
        User contactUser = userRepository.findById(contactUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact user not found"));

        // Check if contact already exists
        contactRepository.findByUserIdAndContactUserId(userId, contactUserId)
                .ifPresent(c -> {
                    throw new BadRequestException("Contact already exists");
                });

        // Cannot add yourself
        if (userId.equals(contactUserId)) {
            throw new BadRequestException("Cannot add yourself as a contact");
        }

        Contact contact = new Contact();
        contact.setUserId(userId);
        contact.setContactUserId(contactUserId);
        contact.setNickname(nickname != null ? nickname : contactUser.getUsername());
        contact.setIsFavorite(false);

        return contactRepository.save(contact);
    }

    /**
     * Get all contacts for a user
     */
    public List<Contact> getUserContacts(Long userId) {
        return contactRepository.findByUserIdOrderByNicknameAsc(userId);
    }

    /**
     * Get favorite contacts
     */
    public List<Contact> getFavoriteContacts(Long userId) {
        return contactRepository.findByUserIdAndIsFavorite(userId, true);
    }

    /**
     * Update contact nickname
     */
    @Transactional
    public Contact updateContactNickname(Long contactId, String nickname) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        contact.setNickname(nickname);
        return contactRepository.save(contact);
    }

    /**
     * Toggle favorite status
     */
    @Transactional
    public Contact toggleFavorite(Long contactId) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        contact.setIsFavorite(!contact.getIsFavorite());
        return contactRepository.save(contact);
    }

    /**
     * Delete a contact
     */
    @Transactional
    public void deleteContact(Long contactId) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        contactRepository.delete(contact);
    }

    /**
     * Search contacts by nickname
     */
    public List<Contact> searchContacts(Long userId, String query) {
        return contactRepository.findByUserIdOrderByNicknameAsc(userId).stream()
                .filter(c -> c.getNickname().toLowerCase().contains(query.toLowerCase()))
                .toList();
    }
}
