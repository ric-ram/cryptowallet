package com.ricram.cryptowallet.dao;

import com.ricram.cryptowallet.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    /**
     * @return each CoinCap slug saved exactly once.
     */
    @Query("select distinct a.slug from Asset a")
    List<String> findDistinctSlugs();
}
