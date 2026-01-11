package com.neurogine.wallet.repository;

import com.neurogine.wallet.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByUserIdOrderByNicknameAsc(Long userId);

    List<Contact> findByUserIdAndIsFavorite(Long userId, Boolean isFavorite);

    Optional<Contact> findByUserIdAndContactUserId(Long userId, Long contactUserId);
}
