package com.ricram.cryptowallet;

import com.ricram.cryptowallet.dao.WalletRepository;
import com.ricram.cryptowallet.dto.CreateWalletRequest;
import com.ricram.cryptowallet.dto.WalletResponseDto;
import com.ricram.cryptowallet.entity.Wallet;
import com.ricram.cryptowallet.service.WalletService;
import com.ricram.cryptowallet.service.impl.WalletServiceImpl;
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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

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
}
