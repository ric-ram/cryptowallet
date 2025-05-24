package com.ricram.cryptowallet;

import com.ricram.cryptowallet.dao.WalletRepository;
import com.ricram.cryptowallet.entity.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class WalletRepositoryTests {
    @Autowired
    WalletRepository walletRepository;

    @Test
    @DisplayName("existsByEmail() -> returns false when no wallet is saved with the provided email")
    void testExistByEmailWhenNoWalletExists() {
        assertThat(walletRepository.existsByEmail("foo@example.com"))
                .isFalse();
    }

    @Test
    @DisplayName("existsByEmail() -> returns true if there is a wallet is saved with that email")
    void testExistByEmailWhenWalletExists() {
        Wallet w = new Wallet();
        w.setEmail("bar@example.com");
        w.setCreateAt(Instant.now());
        walletRepository.saveAndFlush(w);

        assertThat(walletRepository.existsByEmail("bar@example.com"))
                .isTrue();
    }
}
