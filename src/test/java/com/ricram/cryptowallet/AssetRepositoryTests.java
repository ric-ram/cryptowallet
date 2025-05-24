package com.ricram.cryptowallet;

import com.ricram.cryptowallet.dao.AssetQuantity;
import com.ricram.cryptowallet.dao.AssetRepository;
import com.ricram.cryptowallet.dao.WalletRepository;
import com.ricram.cryptowallet.entity.Asset;
import com.ricram.cryptowallet.entity.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
public class AssetRepositoryTests {
    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Test
    @DisplayName("findDistinctSlugs() -> returns unique slugs across all assets")
    void testFindDistinctSlugs() {
        // given: a wallet with duplicate‐slug assets
        Wallet walletToSave = Wallet.builder()
                .email("test@example.com")
                .build();
        Wallet savedWallet = walletRepository.save(walletToSave);

        Asset a1 = Asset.builder()
                .wallet(savedWallet)
                .symbol("BTC")
                .slug("bitcoin")
                .purchasedPrice(new BigDecimal("1000.0"))
                .quantity(7.0)
                .build();
        Asset a2 = Asset.builder()
                .wallet(savedWallet)
                .symbol("BTC")
                .slug("bitcoin")
                .purchasedPrice(new BigDecimal("1500.0"))
                .quantity(4.0)
                .build();
        Asset a3 = Asset.builder()
                .wallet(savedWallet)
                .symbol("ETH")
                .slug("ethereum")
                .purchasedPrice(new BigDecimal("500.0"))
                .quantity(2.0)
                .build();
        assetRepository.saveAll(List.of(a1, a2, a3));

        List<String> slugs = assetRepository.findDistinctSlugs();

        // then: only two distinct slugs in any order
        assertThat(slugs)
                .hasSize(2)
                .containsExactlyInAnyOrder("bitcoin", "ethereum");
    }

    @Test
    @DisplayName("findQuantitiesByWalletId() -> groups and sums quantities per symbol")
    void testFindQuantitiesByWalletId() {
        // given: a wallet with duplicate symbols
        Wallet walletToSave = Wallet.builder()
                .email("test@example.com")
                .build();
        Wallet savedWallet = walletRepository.save(walletToSave);

        Asset btc1 = Asset.builder()
                .wallet(savedWallet)
                .symbol("BTC")
                .slug("bitcoin")
                .purchasedPrice(new BigDecimal("1000.0"))
                .quantity(7.0)
                .build();
        Asset btc2 = Asset.builder()
                .wallet(savedWallet)
                .symbol("BTC")
                .slug("bitcoin")
                .purchasedPrice(new BigDecimal("1500.0"))
                .quantity(4.0)
                .build();
        Asset eth = Asset.builder()
                .wallet(savedWallet)
                .symbol("ETH")
                .slug("ethereum")
                .purchasedPrice(new BigDecimal("500.0"))
                .quantity(2.0)
                .build();
        assetRepository.saveAll(List.of(btc1, btc2, eth));

        List<AssetQuantity> result =
                assetRepository.findQuantitiesByWalletId(savedWallet.getId());

        // then: two entries, BTC summed to 11.0 and ETH to 2.0
        assertThat(result)
                .hasSize(2)
                .anySatisfy(q -> {
                    assertThat(q.getSymbol()).isEqualTo("BTC");
                    assertThat(q.getTotalQuantity()).isEqualTo(11.0);
                })
                .anySatisfy(q -> {
                    assertThat(q.getSymbol()).isEqualTo("ETH");
                    assertThat(q.getTotalQuantity()).isEqualTo(2.0);
                });
    }

}
