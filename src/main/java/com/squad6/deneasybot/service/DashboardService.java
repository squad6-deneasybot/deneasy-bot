package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.DashboardMetricsDTO;
import com.squad6.deneasybot.repository.CompanyRepository;
import com.squad6.deneasybot.repository.EvaluationRepository;
import com.squad6.deneasybot.repository.UserRepository;
import com.squad6.deneasybot.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final EvaluationRepository evaluationRepository;
    private final WishlistRepository wishlistRepository;

    public DashboardService(CompanyRepository companyRepository,
                            UserRepository userRepository,
                            EvaluationRepository evaluationRepository,
                            WishlistRepository wishlistRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.evaluationRepository = evaluationRepository;
        this.wishlistRepository = wishlistRepository;
    }

    @Transactional(readOnly = true)
    public DashboardMetricsDTO getDashboardMetrics() {
        long totalCompanies = companyRepository.count();
        long totalUsers = userRepository.count();
        long totalFeedbacks = evaluationRepository.count() + wishlistRepository.count();

        Double averageRating = evaluationRepository.findAverageRating();
        if (averageRating == null) {
            averageRating = 0.0;
        }

        return new DashboardMetricsDTO(totalCompanies, totalUsers, totalFeedbacks, averageRating);
    }
}