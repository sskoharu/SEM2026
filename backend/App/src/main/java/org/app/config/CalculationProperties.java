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
        private BigDecimal tasaAnual;
        private boolean verificada;

        public boolean isVerificada() {
            return verificada;
        }

        public void setVerificada(boolean verificada) {
            this.verificada = verificada;
        }

        public BigDecimal getTasaAnual() {
            return tasaAnual;
        }

        public void setTasaAnual(BigDecimal tasaAnual) {
            if (tasaAnual == null || tasaAnual.signum() < 0 || tasaAnual.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("La tasa anual debe estar entre 0 y 1.");
            }
            this.tasaAnual = tasaAnual;
        }
    }
}
