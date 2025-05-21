package com.ricram.cryptowallet;

import com.ricram.cryptowallet.dao.AssetRepository;
import com.ricram.cryptowallet.dao.WalletRepository;
import com.ricram.cryptowallet.dto.AddAssetRequest;
import com.ricram.cryptowallet.dto.AssetResponseDto;
import com.ricram.cryptowallet.entity.Asset;
import com.ricram.cryptowallet.entity.Wallet;
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
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowableOfType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AssetServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private AssetRepository assetRepository;

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
        verifyNoMoreInteractions(walletRepository, assetRepository);
    }

    @Test
    @DisplayName("addAsset() -> Saves asset and returns DTO on success")
    void addAssetWithValidWalletId() {

        long validWalletId = 1L;
        Wallet wallet = new Wallet();
        wallet.setId(validWalletId);
        when(walletRepository.findById(validWalletId)).thenReturn(Optional.of(wallet));

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        when(assetRepository.save(captor.capture()))
                .thenAnswer(invocation -> {
                    Asset a = invocation.getArgument(0);
                    a.setId(10L);
                    a.setCreateAt(Instant.parse("2025-05-21T12:00:00Z"));
                    return a;
                });
        BigDecimal price = new BigDecimal("10000.0");
        AddAssetRequest req = new AddAssetRequest("eth", price, 3.0);

        AssetResponseDto dto = assetService.addAsset(validWalletId, req);

        assertEquals(10L, dto.id());
        assertEquals(dto.symbol(), "ETH");
        assertEquals(0, dto.price().compareTo(price));
        assertEquals(dto.quantity(), 3.0);

        Asset saved = captor.getValue();
        assertEquals(saved.getWallet(), wallet);
        assertEquals(saved.getSymbol(), "ETH");
        assertEquals(0, saved.getPrice().compareTo(price));
        assertEquals(saved.getQuantity(), 3.0);
        assertNotNull(saved.getCreateAt());

        verify(walletRepository).findById(validWalletId);
        verify(assetRepository).save(any(Asset.class));
        verifyNoMoreInteractions(walletRepository, assetRepository);

    }

}
