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
        String monto = "A determinar por la autoridad competente";
        String planAmount = org.app.services.layer_4.resolver.PlanRenderer.mainAmount(session.getExecutionPlan());
        if (planAmount != null) {
            monto = planAmount;
        } else if (session.getCollectedParameters().containsKey("pricePaid")) {
            monto = String.valueOf(session.getCollectedParameters().get("pricePaid"));
        }

        // 3. Transformar Evidencias Booleanas a Lista de Textos
        List<String> evidencias = buildEvidenceList(session.getEvidence());

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
                .listaEvidencias(evidencias)
                // hechosRedactados y pretensionesRedactadas se llenarán en la Subtarea 2
                .build();
    }

    List<String> buildEvidenceList(SessionObject.Evidence ev) {
        List<String> evidencias = new ArrayList<>();
        if (ev == null) return evidencias;

        if (ev.isHasInvoice()) evidencias.add("Copia de la factura de compra");
        if (ev.isHasPaymentReceipt()) evidencias.add("Comprobante de pago o transferencia");
        if (ev.isHasContract()) evidencias.add("Contrato suscrito con el proveedor");
        if (ev.isHasWarrantyDocument()) evidencias.add("Documento de garantía");
        if (ev.isHasUserManual()) evidencias.add("Manual o instrucciones de uso");
        if (ev.isHasEntryNote()) evidencias.add("Nota de ingreso al taller o servicio técnico");
        if (ev.isHasPhotosOrVideos()) evidencias.add("Fotografías o videos del defecto o daño");
        if (ev.isHasBillingHistory()) evidencias.add("Planillas o historial de facturación");
        if (ev.isHasAdvertisingEvidence()) evidencias.add("Captura o fotografía de la publicidad");
        if (ev.isHasPriceTagEvidence()) evidencias.add("Fotografía del precio exhibido");
        if (ev.isHasLabelingEvidence()) evidencias.add("Fotografía del rotulado o etiqueta del producto");
        if (ev.isHasWrittenCommunication()) evidencias.add("Comunicaciones escritas con el proveedor (correos, mensajes o cartas)");
        if (ev.isHasComplaintBookRecord()) evidencias.add("Constancia o registro del libro de reclamos");
        if (ev.isHasWitnesses()) evidencias.add("Testigos dispuestos a declarar");
        String other = ev.getOtherEvidenceDescription();
        if (other != null && !other.isBlank()) evidencias.add(other.trim());
        return evidencias;
    }

    public void injectDraftingResult(DocumentContext context, DraftingResult result) {
        if (result != null) {
            context.setHechosRedactados(result.hechosRedactados());
            context.setPretensionesRedactadas(result.pretensionesRedactadas());
        }
    }
}