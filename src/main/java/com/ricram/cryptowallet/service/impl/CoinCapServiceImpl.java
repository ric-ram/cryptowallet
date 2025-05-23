package com.ricram.cryptowallet.service.impl;

import com.ricram.cryptowallet.dto.AssetInfo;
import com.ricram.cryptowallet.dto.AssetListResponseDto;
import com.ricram.cryptowallet.dto.CoinCapAsset;
import com.ricram.cryptowallet.service.CoinCapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoinCapServiceImpl implements CoinCapService {

    private final WebClient coinCapWebClient;


    @Override
    public Optional<AssetInfo> fetchAssetBySymbol(String symbol) {

        String upperCaseSymbol = symbol.trim().toUpperCase();

        try {
            AssetListResponseDto resp = coinCapWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v3/assets")
                            .queryParam("search", upperCaseSymbol)
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .bodyToMono(AssetListResponseDto.class)
                    .block();

            if (resp == null || resp.data() == null || resp.data().isEmpty()) {
                return Optional.empty();
            }

            CoinCapAsset coinCap = resp.data().get(0);
            BigDecimal price = new BigDecimal(coinCap.priceUsd());
            return Optional.of(new AssetInfo(coinCap.id(), coinCap.symbol(), price));
        } catch (WebClientResponseException.NotFound notFound) {
            // 404 -> No such symbol exists
            return Optional.empty();

        } catch (WebClientResponseException e) {
            // 4xx or 5xx from CoinCap
            log.error("CoinCap returned error status {}: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to fetch data from CoinCap",
                    e
            );

        } catch (Exception e) {
            // JSON parsing errors, timeouts, connectivity issue, etc.
            log.error("Unexpected error calling CoinCap", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unexpected error fetching asset info",
                    e
            );
        }
    }

    @Override
    public Optional<AssetInfo> fetchAssetBySlug(String slug) {
        try {
            AssetListResponseDto resp = coinCapWebClient.get()
                    .uri("/v3/assets/{slug}", slug)
                    .retrieve()
                    .bodyToMono(AssetListResponseDto.class)
                    .block();

            if (resp == null || resp.data() == null || resp.data().isEmpty()) {
                return Optional.empty();
            }

            CoinCapAsset coinCap = resp.data().get(0);
            BigDecimal price = new BigDecimal(coinCap.priceUsd());
            return Optional.of(new AssetInfo(coinCap.id(), coinCap.symbol(), price));
        } catch (WebClientResponseException.NotFound notFound) {
            // 404 -> No such symbol exists
            return Optional.empty();

        } catch (WebClientResponseException e) {
            // 4xx or 5xx from CoinCap
            log.error("CoinCap returned error status {}: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to fetch data from CoinCap",
                    e
            );

        } catch (Exception e) {
            // JSON parsing errors, timeouts, connectivity issue, etc.
            log.error("Unexpected error calling CoinCap", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unexpected error fetching asset info",
                    e
            );
        }
    }
}
