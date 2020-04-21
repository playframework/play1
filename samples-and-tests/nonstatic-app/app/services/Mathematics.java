package services;

import javax.inject.Singleton;
import java.math.BigDecimal;

@Singleton
public class Mathematics {
  public BigDecimal sqrt(BigDecimal number) {
    return new BigDecimal(Math.sqrt(number.doubleValue()));
  }
}
