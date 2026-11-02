package org.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "calculos")
public class CalculationProperties {

    private final Interes interes = new Interes();

    public Interes getInteres() {
        return interes;
    }

    public static class Interes {
        private BigDecimal tasaAnual = new BigDecimal("0.12");

        public BigDecimal getTasaAnual() {
            return tasaAnual;
        }

        public void setTasaAnual(BigDecimal tasaAnual) {
            this.tasaAnual = tasaAnual;
        }
    }
}
