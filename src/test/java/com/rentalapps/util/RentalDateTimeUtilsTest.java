package com.rentalapps.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RentalDateTimeUtilsTest {

  @Test
  void getTwoDigitDayMonthRequiresYear() {
    assertThatThrownBy(() -> RentalDateTimeUtils.getTwoDigitDayMonth("07/02"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("MM/dd/yyyy");
  }

  @Test
  void getTwoDigitDayMonthPadsMonthAndDay() {
    assertThat(RentalDateTimeUtils.getTwoDigitDayMonth("7/2/2026")).isEqualTo("07/02/2026");
  }
}
