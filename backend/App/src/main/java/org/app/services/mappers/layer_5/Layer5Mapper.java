package org.app.services.mappers.layer_5;

import org.app.schemas.session_object.SessionObject;
import org.app.services.layer_5.dto.DocumentContext;
import org.app.services.layer_5.dto.DraftingResult;
import org.app.services.layer_5.dto.Layer5RequestDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Layer5Mapper {

    public DocumentContext mapToDocumentContext(SessionObject session, Layer5RequestDto request) {
        // 1. Formatear Artículos
        String articulos = String.join(", ", session.getApplicableArticles());

        // 2. Extraer Cuantía del ExecutionPlan (si existe)
        String monto = "A determinar";
        if (session.getExecutionPlan() != null && session.getExecutionPlan().containsKey("monto_total_restitucion")) {
            monto = String.valueOf(session.getExecutionPlan().get("monto_total_restitucion"));
        } else if (session.getCollectedParameters().containsKey("pricePaid")) {
            monto = String.valueOf(session.getCollectedParameters().get("pricePaid"));
        }

        // 3. Transformar Evidencias Booleanas a Lista de Textos
        /*List<String> evidencias = new ArrayList<>();
        if (session.getEvidence() != null) {
            if (session.getEvidence().isHasInvoice()) evidencias.add("Copia de Factura");
            if (session.getEvidence().isHasVoucher()) evidencias.add("Comprobante de Pago");
            if (session.getEvidence().isHasAdditionalReceipt()) evidencias.add("Comprobante Adicional");
        }*/

        // 4. Construir el Contexto
        return DocumentContext.builder()
                .consumerFullName(request.getFullName())
                .consumerIdCard(request.getIdCard())
                .consumerContactInfo(request.getContactInfo())
                .providerName(request.getProviderCommercialName())
                .providerRuc(request.getProviderRuc())
                .providerAddress(request.getProviderAddress())
                .applicableArticles(articulos)
                .cuantia(monto)
                .listaEvidencias(null)
                // hechosRedactados y pretensionesRedactadas se llenarán en la Subtarea 2
                .build();
    }

    public void injectDraftingResult(DocumentContext context, DraftingResult result) {
        if (result != null) {
            context.setHechosRedactados(result.hechosRedactados());
            context.setPretensionesRedactadas(result.pretensionesRedactadas());
        }
    }
}