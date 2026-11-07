package org.app.schemas.session_object.enums;

    public enum ClaimType {

        //VERIFIED
        VICIOS_OCULTOS_CALIDAD(
                1,
                "Bienes Defectuosos y Vicios Ocultos",
                "Arts. 20, 26, 71"
        ),

        //VERIFIED
        SERVICIO_TECNICO_CUSTODIA(
                2,
                "Defectos en Servicios Técnicos y Custodia",
                "Arts. 22, 23, 24, 25, 73, 75"
        ),

        //VERIFIED
        INCUMPLIMIENTO_CONTRATOS_MORA(
                3,
                "Incumplimiento de Contratos, Ofertas y Mora",
                "Arts. 18, 21, 30, 46, 55(num. 7), 76, 77, 148"
                // Nota: Art. 21 incs. 2 y 3
        ),

        //VERIFIED
        INFORMACION_PRECIOS_FACTURACION(
                4,
                "Información Deficiente, Precios y Facturación",
                "Arts. 6, 7, 8, 9, 12, 13, 14, 15, 16, 17, 19, 21 (inciso 1), 27, 72, 91, 93"
        ),

        //VERIFIED
        SERVICIOS_PUBLICOS_DOMICILIARIOS(
                5,
                "Servicios Públicos Domiciliarios",
                "Arts. 32, 33, 34, 35, 36, 37, 38, 39, 40, 78"
        ),

        //VERIFIED
        CONTRATOS(
                6,
                "Contratos de Adhesión",
                "Art. 44"
        ),

        //VERIFIED
        FINANCIEROS_CREDITOS_COBRANZAS(
                7,
                "Servicios Financieros, Créditos y Cobranzas",
                "Arts. 47, 48, 49, 50, 55(num. 8)"

        ),

        //VERIFIED
        PRACTICAS_ABUSIVAS_MERCADO(
                8,
                "Prácticas Abusivas de Mercado",
                "Arts. 55"
        );

        private final int code;
        private final String displayName;
        private final String legalBasis;

        ClaimType(int code, String displayName, String legalBasis) {
            this.code = code;
            this.displayName = displayName;
            this.legalBasis = legalBasis;
        }

        public int getCode() {
            return code;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getLegalBasis() {
            return legalBasis;
        }

        // Método útil para buscar el enum desde el Agente Triaje (Capa 1)
        public static ClaimType fromCode(int code) {
            for (ClaimType type : ClaimType.values()) {
                if (type.code == code) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Código de tipo de reclamo LODC inválido: " + code);
        }
    }