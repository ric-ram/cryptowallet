package com.ricram.cryptowallet.dao;

import com.ricram.cryptowallet.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    /**
     * @return each CoinCap slug saved exactly once.
     */
    @Query("select distinct a.slug from Asset a")
    List<String> findDistinctSlugs();

    @Query("""
            SELECT a.slug AS slug,
                   a.symbol AS symbol,
                   SUM(a.quantity) AS totalQuantity
            FROM Asset a
            WHERE a.wallet.id = :walletId
            GROUP BY a.slug, a.symbol
            """)
    List<AssetQuantity> findQuantitiesByWalletId(@Param("walletId") Long walletId);
}
