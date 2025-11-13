package com.squad6.deneasybot.model;

import java.time.OffsetDateTime;

public record EvaluationDTO(
        Long id,
        String content,
        int rating,
        OffsetDateTime createdAt,
        String userName,
        String companyName
) {
    public EvaluationDTO(Evaluation evaluation) {
        this(
                evaluation.getId(),
                evaluation.getContent(),
                evaluation.getRating(),
                evaluation.getCreatedAt(),
                evaluation.getUser() != null ? evaluation.getUser().getName() : "Usuário Anônimo",
                (evaluation.getUser() != null && evaluation.getUser().getCompany() != null) ?
                        evaluation.getUser().getCompany().getName() : "Empresa Desconhecida"
        );
    }
}