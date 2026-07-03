package com.rentalapps.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RentalDateTimeUtilsTest {

  @Test
  void getTwoDigitDayMonthFormatsMonthDayYear() {
    assertThat(RentalDateTimeUtils.getTwoDigitDayMonth("7/2/2026")).isEqualTo("07/02/2026");
  }

  @Test
  void getTwoDigitDayMonthAddsCurrentYearForMonthDay() {
    assertThat(RentalDateTimeUtils.getTwoDigitDayMonth("7/2"))
        .isEqualTo("07/02/" + LocalDate.now().getYear());
  }
}
