package com.ricram.cryptowallet.dao;

import com.ricram.cryptowallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    boolean existsByEmail(String email);
}
