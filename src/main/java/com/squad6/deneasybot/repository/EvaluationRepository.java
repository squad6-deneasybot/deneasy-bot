package com.squad6.deneasybot.repository;

import com.squad6.deneasybot.model.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;


public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findAllByOrderByCreatedAtDesc();
    @Query("SELECT AVG(e.rating) FROM Evaluation e")
    Double findAverageRating();
}