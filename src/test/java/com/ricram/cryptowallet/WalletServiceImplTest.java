package com.ricram.cryptowallet;

import com.ricram.cryptowallet.dao.AssetQuantity;
import com.ricram.cryptowallet.dao.AssetRepository;
import com.ricram.cryptowallet.dao.LatestPriceRepository;
import com.ricram.cryptowallet.dao.WalletRepository;
import com.ricram.cryptowallet.dto.AssetValue;
import com.ricram.cryptowallet.dto.CreateWalletRequest;
import com.ricram.cryptowallet.dto.WalletResponseDto;
import com.ricram.cryptowallet.dto.WalletValuationResponseDto;
import com.ricram.cryptowallet.entity.LatestPrice;
import com.ricram.cryptowallet.entity.Wallet;
import com.ricram.cryptowallet.service.WalletService;
import com.ricram.cryptowallet.service.impl.WalletServiceImpl;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.checkerframework.checker.units.qual.C;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private LatestPriceRepository latestPriceRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    @DisplayName("create() -> Conflict when email already exists")
    void createWithDuplicateEmail() {

        // arrange
        String email = "test@example.com";
        when(walletRepository.existsByEmail(email)).thenReturn(true);
        CreateWalletRequest req = new CreateWalletRequest(email);

        // act & assert
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> walletService.create(req)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("already in use"));

        verify(walletRepository).existsByEmail(email);
        verifyNoMoreInteractions(walletRepository);
    }

    @Test
    @DisplayName("create() -> Success when email is new, returns correct DTO")
    void createWithNewEmail() {

        // arrange
        String email = "testing@example.com";
        when(walletRepository.existsByEmail(email)).thenReturn(false);

        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        when(walletRepository.save(captor.capture()))
                .thenAnswer(invocation -> {
                    Wallet w = invocation.getArgument(0);
                    w.setId(53L);
                    w.setCreateAt(Instant.parse("2025-05-21T12:00:00Z"));
                    return w;
                });
        CreateWalletRequest req = new CreateWalletRequest(email);

        // act
        WalletResponseDto dto = walletService.create(req);

        // assert
        assertEquals(53L, dto.id());
        assertEquals(email, dto.email());

        Wallet saved = captor.getValue();
        assertEquals(email, dto.email());
        assertNotNull(saved.getCreateAt());

        verify(walletRepository).existsByEmail(email);
        verify(walletRepository).save(any(Wallet.class));
        verifyNoMoreInteractions(walletRepository);

    }

    @Test
    @DisplayName("getValuation() -> 404 when walletId does not exist")
    void getValuationInvalidWalletId() {

        Long invalidId = 99L;
        when(walletRepository.findById(invalidId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> walletService.getValuation(invalidId)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("not found"));

        verify(walletRepository).findById(invalidId);
        verifyNoMoreInteractions(walletRepository, assetRepository, latestPriceRepository);
    }

    @Test
    @DisplayName("getValuation() -> 503 when price is not found for an asset")
    void getValuationNoAssetPrice() {

        Wallet wallet = new Wallet(1L, "test@example.com", Instant.now());
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        AssetQuantity qty = new AssetQuantity() {
            @Override
            public String getSlug() {
                return "bitcoin";
            }

            @Override
            public String getSymbol() {
                return "BTC";
            }

            @Override
            public double getTotalQuantity() {
                return 3.0;
            }
        };
        when(assetRepository.findQuantitiesByWalletId(wallet.getId()))
                .thenReturn(List.of(qty));

        when(latestPriceRepository.findBySlug("bitcoin"))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> walletService.getValuation(wallet.getId())
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        assertTrue(ex.getReason().contains("No recorded price"));

        verify(walletRepository).findById(wallet.getId());
        verify(assetRepository).findQuantitiesByWalletId(wallet.getId());
        verify(latestPriceRepository).findBySlug("bitcoin");
        verifyNoMoreInteractions(walletRepository, assetRepository, latestPriceRepository);
    }

    @Test
    @DisplayName("getValuation() -> Success returning the wallet valuation")
    void getValuationSuccess() {

        Wallet wallet = new Wallet(1L, "test@example.com", Instant.now());
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        AssetQuantity qtyBtc = new AssetQuantity() {
            @Override
            public String getSlug() {
                return "bitcoin";
            }

            @Override
            public String getSymbol() {
                return "BTC";
            }

            @Override
            public double getTotalQuantity() {
                return 4.0;
            }
        };
        AssetQuantity qtyEth = new AssetQuantity() {
            @Override
            public String getSlug() {
                return "ethereum";
            }

            @Override
            public String getSymbol() {
                return "ETH";
            }

            @Override
            public double getTotalQuantity() {
                return 3.0;
            }
        };
        when(assetRepository.findQuantitiesByWalletId(wallet.getId()))
                .thenReturn(List.of(qtyBtc, qtyEth));

        LatestPrice btcPrice = LatestPrice.builder()
                .slug("bitcoin")
                .symbol("BTC")
                .price(new BigDecimal("10000.0"))
                .fetchedAt(Instant.now())
                .build();
        LatestPrice ethPrice = LatestPrice.builder()
                .slug("ethereum")
                .symbol("ETH")
                .price(new BigDecimal("4000.0"))
                .fetchedAt(Instant.now())
                .build();
        when(latestPriceRepository.findBySlug("bitcoin"))
                .thenReturn(Optional.of(btcPrice));
        when(latestPriceRepository.findBySlug("ethereum"))
                .thenReturn(Optional.of(ethPrice));

        WalletValuationResponseDto dto = walletService.getValuation(wallet.getId());

        assertEquals(dto.id(), wallet.getId());

        List<AssetValue> assetValues = dto.assets();
        assertThat(assetValues).hasSize(2);

        AssetValue btc = assetValues.get(0);
        assertEquals(btc.symbol(), "BTC");
        assertEquals(btc.quantity(), 4.0);
        assertThat(btc.price()).isEqualByComparingTo("10000.0");
        assertThat(btc.value()).isEqualByComparingTo("40000.0");

        AssetValue eth = assetValues.get(1);
        assertEquals(eth.symbol(), "ETH");
        assertEquals(eth.quantity(), 3.0);
        assertThat(eth.price()).isEqualByComparingTo("4000.0");
        assertThat(eth.value()).isEqualByComparingTo("12000.0");

        assertThat(dto.total()).isEqualByComparingTo("52000.0");

        verify(walletRepository).findById(wallet.getId());
        verify(assetRepository).findQuantitiesByWalletId(wallet.getId());
        verify(latestPriceRepository).findBySlug("bitcoin");
        verify(latestPriceRepository).findBySlug("ethereum");
        verifyNoMoreInteractions(walletRepository, assetRepository, latestPriceRepository);
    }
}
