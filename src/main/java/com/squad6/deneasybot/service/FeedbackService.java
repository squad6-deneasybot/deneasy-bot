package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.User;
import com.squad6.deneasybot.model.Wishlist;
import com.squad6.deneasybot.model.WishlistDTO;
import com.squad6.deneasybot.repository.WishlistRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FeedbackService {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackService.class);
    private final WishlistRepository wishlistRepository;

    public FeedbackService(WishlistRepository wishlistRepository) {
        this.wishlistRepository = wishlistRepository;
    }

    @Transactional
    public void saveWishlist(User user, String content) {
        logger.info("Salvando Wishlist para o usuário: {}", user.getEmail());
        Wishlist wish = new Wishlist();
        wish.setUser(user);
        wish.setContent(content);
        wishlistRepository.save(wish);
    }
    @Transactional(readOnly = true)
    public List<WishlistDTO> getAllWishlistItems() {
        return wishlistRepository.findAllWithUserDetails();
    }
}