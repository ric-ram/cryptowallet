package com.ricram.cryptowallet.dao;

import com.ricram.cryptowallet.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, Long> {

}
