package com.majstornaklik.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "base_token_cost")
    @Builder.Default
    private Integer baseTokenCost = 1;
}
