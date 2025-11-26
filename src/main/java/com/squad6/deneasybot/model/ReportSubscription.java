package com.squad6.deneasybot.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "report_subscription_table")
public class ReportSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequency frequency;

    @Column(name = "last_sent_at")
    private LocalDate lastSentAt;

    public ReportSubscription() {}

    public ReportSubscription(User user, Frequency frequency) {
        this.user = user;
        this.frequency = frequency;
        this.lastSentAt = LocalDate.now().minusDays(40);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Frequency getFrequency() { return frequency; }
    public void setFrequency(Frequency frequency) { this.frequency = frequency; }
    public LocalDate getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(LocalDate lastSentAt) { this.lastSentAt = lastSentAt; }
}