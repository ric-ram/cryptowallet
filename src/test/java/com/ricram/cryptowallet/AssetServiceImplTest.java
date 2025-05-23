package com.ricram.cryptowallet;

import com.ricram.cryptowallet.dao.AssetRepository;
import com.ricram.cryptowallet.dao.LatestPriceRepository;
import com.ricram.cryptowallet.dao.WalletRepository;
import com.ricram.cryptowallet.dto.AddAssetRequest;
import com.ricram.cryptowallet.dto.AssetInfo;
import com.ricram.cryptowallet.dto.AssetResponseDto;
import com.ricram.cryptowallet.entity.Asset;
import com.ricram.cryptowallet.entity.LatestPrice;
import com.ricram.cryptowallet.entity.Wallet;
import com.ricram.cryptowallet.service.CoinCapService;
import com.ricram.cryptowallet.service.impl.AssetServiceImpl;
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
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AssetServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private LatestPriceRepository latestPriceRepository;

    @Mock
    private CoinCapService coinCapService;

    @InjectMocks
    private AssetServiceImpl assetService;

    @Test
    @DisplayName("addAsset() -> 404 when wallet not found")
    void addAssetWithInvalidWalletId() {

        long invalidWalletId = 55L;
        when(walletRepository.findById(invalidWalletId)).thenReturn(Optional.empty());
        AddAssetRequest req = new AddAssetRequest("btc", new BigDecimal("10000.0"), 4.0);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> assetService.addAsset(invalidWalletId, req)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Wallet not found"));

        verify(walletRepository).findById(invalidWalletId);
        verifyNoMoreInteractions(walletRepository, assetRepository, coinCapService);
    }

    @Test
    @DisplayName("addAsset() -> Saves asset and returns DTO on success fetching data from CoinCap")
    void addAssetWithValidWalletIdAndValidSymbol() {

        long validWalletId = 1L;
        Wallet wallet = new Wallet();
        wallet.setId(validWalletId);
        when(walletRepository.findById(validWalletId)).thenReturn(Optional.of(wallet));

        BigDecimal price = new BigDecimal("10000.0");
        AssetInfo info = new AssetInfo("bitcoin", "BTC", price);
        when(coinCapService.fetchAssetBySymbol("btc")).thenReturn(Optional.of(info));

        LatestPrice existing = LatestPrice.builder()
                .id(10L)
                .slug("bitcoin")
                .symbol("BTC")
                .price(new BigDecimal("12000.0"))
                .fetchedAt(Instant.now())
                .build();
        when(latestPriceRepository.findBySlug("bitcoin")).thenReturn(Optional.of(existing));

        ArgumentCaptor<LatestPrice> priceCaptor = ArgumentCaptor.forClass(LatestPrice.class);
        when(latestPriceRepository.save(priceCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
        when(assetRepository.save(assetCaptor.capture()))
                .thenAnswer(invocation -> {
                    Asset a = invocation.getArgument(0);
                    a.setId(10L);
                    a.setCreateAt(Instant.parse("2025-05-21T12:00:00Z"));
                    return a;
                });

        BigDecimal purchasedPrice = new BigDecimal("9000.0");
        AddAssetRequest req = new AddAssetRequest("btc", purchasedPrice, 3.0);

        AssetResponseDto dto = assetService.addAsset(validWalletId, req);

        assertEquals(10L, dto.id());
        assertEquals(dto.symbol(), "BTC");
        assertEquals(0, dto.price().compareTo(purchasedPrice));
        assertEquals(dto.quantity(), 3.0);

        LatestPrice upserted = priceCaptor.getValue();
        assertEquals(upserted.getSlug(), "bitcoin");
        assertEquals(upserted.getSymbol(), "BTC");
        assertEquals(0, upserted.getPrice().compareTo(price));
        assertThat(upserted.getFetchedAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));

        Asset saved = assetCaptor.getValue();
        assertEquals(saved.getWallet(), wallet);
        assertEquals(saved.getSymbol(), "BTC");
        assertEquals(saved.getSlug(), "bitcoin");
        assertEquals(0, saved.getPurchasedPrice().compareTo(purchasedPrice));
        assertEquals(saved.getQuantity(), 3.0);
        assertNotNull(saved.getCreateAt());

        verify(walletRepository).findById(validWalletId);
        verify(assetRepository).save(any(Asset.class));
        verify(coinCapService).fetchAssetBySymbol("btc");
        verifyNoMoreInteractions(walletRepository, assetRepository, coinCapService);

    }

    @Test
    @DisplayName("addAsset() -> 400 when CoinCap returns empty")
    void addAssetWithInvalidSymbol() {

        long validWalletId = 1L;
        Wallet wallet = new Wallet();
        wallet.setId(validWalletId);
        when(walletRepository.findById(validWalletId)).thenReturn(Optional.of(wallet));
        when(coinCapService.fetchAssetBySymbol("foo")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> assetService.addAsset(1L, new AddAssetRequest("foo", new BigDecimal("1000.0"), 3.0))
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Unknown asset"));

        verify(coinCapService).fetchAssetBySymbol("foo");
        verifyNoMoreInteractions(assetRepository, coinCapService, walletRepository);
    }

}
