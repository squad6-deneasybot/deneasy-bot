package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.Evaluation;
import com.squad6.deneasybot.model.EvaluationDTO;
import com.squad6.deneasybot.model.UserProfile;
import com.squad6.deneasybot.repository.EvaluationRepository;
import org.springframework.security.access.AccessDeniedException;

import com.squad6.deneasybot.model.User;
import com.squad6.deneasybot.model.Wishlist;
import com.squad6.deneasybot.model.WishlistDTO;
import com.squad6.deneasybot.repository.WishlistRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FeedbackService {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackService.class);
    private final WishlistRepository wishlistRepository;

    private final EvaluationRepository evaluationRepository;

    public FeedbackService(WishlistRepository wishlistRepository, EvaluationRepository evaluationRepository) {
        this.wishlistRepository = wishlistRepository;
        this.evaluationRepository = evaluationRepository;
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

    @Transactional
    public Evaluation saveEvaluation(User user, String content, int rating) {
        logger.info("Salvando Evaluation (Rating: {}) para o usuário: {}", rating, user.getEmail());
        Evaluation evaluation = new Evaluation(content, rating, user);
        return evaluationRepository.save(evaluation);
    }

    @Transactional(readOnly = true)
    public List<EvaluationDTO> getAllEvaluations(User manager) {
        if (manager.getProfile() != UserProfile.MANAGER) {
            throw new AccessDeniedException("Acesso negado. Apenas gestores podem visualizar as avaliações.");
        }

        return evaluationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(EvaluationDTO::new)
                .collect(Collectors.toList());
    }
}