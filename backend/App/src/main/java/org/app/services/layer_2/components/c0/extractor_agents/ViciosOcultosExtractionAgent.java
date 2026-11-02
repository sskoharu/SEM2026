package org.app.services.layer_2.components.c0.extractor_agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class ViciosOcultosExtractionAgent extends BaseExtractionAgent {

    public ViciosOcultosExtractionAgent(ChatClient.Builder builder, ObjectMapper objectMapper) {
        super(builder, objectMapper);
    }

    @Override
    public String getSupportedClaimType() {
        return "VICIOS_OCULTOS_CALIDAD";
    }

    @Override
    protected String getExtractionRules() {
        return """
               - purchaseDate: Fecha de compra.
               - pricePaid: Monto pagado.
               - defectDescription: Falla del producto.
               - hasInvoice: Si tiene factura.
               - hasWarranty: Si tiene garantía.
               - thirdPartyIntervention: Si un tercero lo manipuló.
               - hasPhotosOrVideos: Si tiene fotos del defecto.
               - hasWarrantyDocument: Si tiene el documento físico de garantía.
               """;
    }

    @Override
    protected String getIrrelevantRules() {
        return """
               - missingWeightOrMeasure: Es irrelevante si el bien es un electrodoméstico o un producto que no se vende a granel.
               """;
    }

    @Override
    protected String getProceduralRules() {
        return """
               - claimsIndemnification
               - art71Causal
               - chosenRemedy
               """;
    }
}