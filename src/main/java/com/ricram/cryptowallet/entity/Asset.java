package com.ricram.cryptowallet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Table(name = "asset")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal purchasedPrice;

    @Column(nullable = false)
    private double quantity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Wallet wallet;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createAt = Instant.now();
}
