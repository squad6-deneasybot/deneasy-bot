package com.squad6.deneasybot.model;

import jakarta.persistence.*;

@Entity
@Table(name = "example")
public class ExampleModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
