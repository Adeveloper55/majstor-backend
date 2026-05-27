package com.majstornaklik.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "token_packages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "token_amount", nullable = false)
    private Integer tokenAmount;

    @Column(name = "price_eur", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceEur;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
