package com.squad6.deneasybot.repository;

import com.squad6.deneasybot.model.ReportSubscription;
import com.squad6.deneasybot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReportSubscriptionRepository extends JpaRepository<ReportSubscription, Long> {
    Optional<ReportSubscription> findByUser(User user);
}