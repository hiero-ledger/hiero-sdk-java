// SPDX-License-Identifier: Apache-2.0
package org.hiero.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class HbarTest {
    private static final long fiftyGTinybar = 5_000_000_000L;
    private final Hbar fiftyHbar = Hbar.fromTinybars(fiftyGTinybar);

    private final Hbar hundredHbar = new Hbar(100);

    private final Hbar negativeFiftyHbar = new Hbar(-50);

    static Iterator<Arguments> getValueConversions() {
        return List.of(
                        Arguments.arguments(new BigDecimal(50_000_000), HbarUnit.MICROBAR),
                        Arguments.arguments(new BigDecimal(50_000), HbarUnit.MILLIBAR),
                        Arguments.arguments(new BigDecimal(50), HbarUnit.HBAR),
                        Arguments.arguments(new BigDecimal("0.05"), HbarUnit.KILOBAR),
                        Arguments.arguments(new BigDecimal("0.00005"), HbarUnit.MEGABAR),
                        Arguments.arguments(new BigDecimal("0.00000005"), HbarUnit.GIGABAR))
                .iterator();
    }

    @Test
    void shouldConstruct() {
        assertThat(fiftyHbar.toTinybars()).isEqualTo(fiftyGTinybar);
        assertThat(fiftyHbar.to(HbarUnit.HBAR)).isEqualTo(new BigDecimal(50));
        assertThat(new Hbar(50).toTinybars()).isEqualTo(fiftyGTinybar);
        assertThat(Hbar.fromTinybars(fiftyGTinybar).toTinybars()).isEqualTo(fiftyGTinybar);
    }

    @Test
    void shouldNotConstruct() {
        assertThatExceptionOfType(Exception.class).isThrownBy(() -> new Hbar(new BigDecimal("0.1"), HbarUnit.TINYBAR));
    }

    @Test
    void shouldDisplay() {
        assertThat(fiftyHbar.toString()).isEqualTo("50 ℏ");
        assertThat(negativeFiftyHbar.toString()).isEqualTo("-50 ℏ");
        assertThat(Hbar.fromTinybars(1).toString()).isEqualTo("1 tℏ");
        assertThat(Hbar.fromTinybars(1).negated().toString()).isEqualTo("-1 tℏ");
        assertThat(Hbar.fromTinybars(1000).toString()).isEqualTo("1000 tℏ");
        assertThat(Hbar.fromTinybars(1000).negated().toString()).isEqualTo("-1000 tℏ");
    }

    @ParameterizedTest
    @MethodSource("getValueConversions")
    void shouldConvert(BigDecimal value, HbarUnit unit) {
        assertThat(Hbar.from(value, unit)).isEqualTo(fiftyHbar);
        assertThat(fiftyHbar.to(unit)).isEqualTo(value);
    }

    @Test
    void shouldCompare() {
        assertThat(fiftyHbar).isEqualTo(fiftyHbar);
        assertThat(fiftyHbar).isNotEqualTo(hundredHbar);

        assertThat(fiftyHbar.compareTo(new Hbar(50))).isEqualTo(0);

        assertThat(fiftyHbar.compareTo(hundredHbar)).isLessThan(0);
        assertThat(hundredHbar.compareTo(fiftyHbar)).isGreaterThan(0);

        assertThat(fiftyHbar.compareTo(negativeFiftyHbar)).isGreaterThan(0);
    }

    @Test
    void constructorWorks() {
        new Hbar(1);
    }

    @Test
    void fromString() {
        assertThat(Hbar.fromString("1").toTinybars()).isEqualTo(100_000_000);
        assertThat(Hbar.fromString("1 ℏ").toTinybars()).isEqualTo(100_000_000);
        assertThat(Hbar.fromString("1.5 mℏ").toTinybars()).isEqualTo(150_000);
        assertThat(Hbar.fromString("+1.5 mℏ").toTinybars()).isEqualTo(150_000);
        assertThat(Hbar.fromString("-1.5 mℏ").toTinybars()).isEqualTo(-150_000);
        assertThat(Hbar.fromString("+3").toTinybars()).isEqualTo(300_000_000);
        assertThat(Hbar.fromString("-3").toTinybars()).isEqualTo(-300_000_000);
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            Hbar.fromString("1 h");
        });
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            Hbar.fromString("1ℏ");
        });
    }

    @Test
    void fromStringUnit() {
        assertThat(Hbar.fromString("1", HbarUnit.TINYBAR).toTinybars()).isEqualTo(1);
    }

    @Test
    void from() {
        assertThat(Hbar.from(1).toTinybars()).isEqualTo(100000000);
    }

    @Test
    void fromUnit() {
        assertThat(Hbar.from(1, HbarUnit.TINYBAR).toTinybars()).isEqualTo(1);
    }

    @Test
    void getValue() {
        assertThat(new Hbar(1).getValue()).isEqualTo(BigDecimal.valueOf(1));
    }

    @Test
    void hasHashCode() {
        assertThat(new Hbar(1).hashCode()).isEqualTo(100000031);
    }
}
