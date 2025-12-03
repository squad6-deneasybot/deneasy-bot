package com.squad6.deneasybot.repository;

import com.squad6.deneasybot.model.ChartDataDTO;
import com.squad6.deneasybot.model.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    List<Evaluation> findAllByOrderByCreatedAtDesc();

    @Query("SELECT AVG(e.rating) FROM Evaluation e")
    Double findAverageRating();

    @Query("SELECT new com.squad6.deneasybot.model.ChartDataDTO(cast(e.createdAt as LocalDate), COUNT(e)) " +
            "FROM Evaluation e " +
            "WHERE e.createdAt >= :startDate " +
            "GROUP BY cast(e.createdAt as LocalDate) " +
            "ORDER BY cast(e.createdAt as LocalDate) ASC")
    List<ChartDataDTO> findEvaluationsCountByDateSince(@Param("startDate") OffsetDateTime startDate);
}