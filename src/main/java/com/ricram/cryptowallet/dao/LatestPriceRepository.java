package com.ricram.cryptowallet.dao;

import com.ricram.cryptowallet.entity.LatestPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LatestPriceRepository extends JpaRepository<LatestPrice, Long> {

   Optional<LatestPrice> findBySlug(String slug);
}
