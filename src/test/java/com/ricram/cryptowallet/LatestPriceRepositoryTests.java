package com.ricram.cryptowallet;

import com.ricram.cryptowallet.dao.LatestPriceRepository;
import com.ricram.cryptowallet.entity.LatestPrice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DataJpaTest
public class LatestPriceRepositoryTests {
    @Autowired
    LatestPriceRepository latestPriceRepository;

    @Test
    @DisplayName("findBySlug() -> returns the saved price")
    void testFindBySymbol() {
        // given
        LatestPrice cp = LatestPrice.builder()
                .symbol("BTC")
                .slug("bitcoin")
                .price(new BigDecimal("50000.00"))
                .fetchedAt(Instant.now())
                .build();
        latestPriceRepository.save(cp);

        // when
        Optional<LatestPrice> found = latestPriceRepository.findBySlug("bitcoin");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getPrice()).isEqualByComparingTo("50000.00");
        assertThat(found.get().getSlug()).isEqualTo("bitcoin");
        assertThat(found.get().getSymbol()).isEqualTo("BTC");
    }

    @Test
    @DisplayName("findBySlug() -> slug unique constraint enforced")
    void testUniqueSlugConstraint() {
        LatestPrice lp1 = LatestPrice.builder()
                .symbol("ETH")
                .slug("ethereum")
                .price(new BigDecimal("2000.00"))
                .fetchedAt(Instant.now())
                .build();
        latestPriceRepository.save(lp1);

        LatestPrice lp2 = LatestPrice.builder()
                .symbol("ETH-DUP")
                .slug("ethereum")
                .price(new BigDecimal("2100.00"))
                .fetchedAt(Instant.now())
                .build();

        // then: saving a duplicate slug should throw an exception
        assertThatThrownBy(() -> latestPriceRepository.saveAndFlush(lp2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
