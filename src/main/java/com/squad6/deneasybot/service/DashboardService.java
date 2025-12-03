package com.squad6.deneasybot.service;

import com.squad6.deneasybot.model.ChartDataDTO;
import com.squad6.deneasybot.model.DashboardMetricsDTO;
import com.squad6.deneasybot.repository.CompanyRepository;
import com.squad6.deneasybot.repository.EvaluationRepository;
import com.squad6.deneasybot.repository.UserRepository;
import com.squad6.deneasybot.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29);
        OffsetDateTime startDateTime = startDate.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());

        List<ChartDataDTO> rawData = evaluationRepository.findEvaluationsCountByDateSince(startDateTime);

        Map<LocalDate, Long> dataMap = rawData.stream()
                .collect(Collectors.toMap(ChartDataDTO::date, ChartDataDTO::count));

        List<ChartDataDTO> filledHistory = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Long count = dataMap.getOrDefault(date, 0L);
            filledHistory.add(new ChartDataDTO(date, count));
        }

        return new DashboardMetricsDTO(totalCompanies, totalUsers, totalFeedbacks, averageRating, filledHistory);
    }
}