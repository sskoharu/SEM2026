package org.app.services.layer_5.c1;

import lombok.extern.log4j.Log4j2;
import org.app.exceptions.ClientException;
import org.springframework.http.HttpStatus;
import org.app.services.layer_5.dto.DocumentContext;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
@Log4j2
public class DocumentGeneratorService {

    /**
     * Toma la plantilla y el contexto, y devuelve un arreglo de bytes con el Word generado.
     */
    public byte[] generateDocument(DocumentContext context) {
        log.info("Iniciando generación del documento Word...");

        // 1. Mapear las variables del contexto a llaves planas (Formato {{llave}})
        Map<String, String> variables = new HashMap<>();
        variables.put("consumer.fullName", context.getConsumerFullName());
        variables.put("consumer.idCard", context.getConsumerIdCard());
        variables.put("consumer.contactInfo", context.getConsumerContactInfo());

        variables.put("provider.commercialName", context.getProviderName());
        variables.put("provider.ruc", context.getProviderRuc());
        variables.put("provider.address", context.getProviderAddress());

        variables.put("hechosRedactados", context.getHechosRedactados());
        variables.put("pretensionesRedactadas", context.getPretensionesRedactadas());
        variables.put("applicableArticles", context.getApplicableArticles());
        variables.put("cuantia", context.getCuantia());

        // Para la lista de evidencias, las unimos con saltos de línea
        String evidenciasTexto = context.getListaEvidencias() != null && !context.getListaEvidencias().isEmpty()
                ? "- " + String.join("\n- ", context.getListaEvidencias())
                : "No se adjuntaron evidencias documentales.";
        variables.put("listaEvidencias", evidenciasTexto);

        // Nota: Si aún tienes {{conflictDetails.eventDate}} en tu plantilla,
        // puedes mapearlo a la fecha del parámetro recolectado (ej. purchaseDate)

        try (InputStream is = new ClassPathResource("templates/Plantilla_Reclamo_LODC.docx").getInputStream();
             XWPFDocument document = new XWPFDocument(is)) {

            // 2. Reemplazar en los párrafos normales
            for (XWPFParagraph p : document.getParagraphs()) {
                replaceInParagraph(p, variables);
            }

            // 3. Reemplazar en las tablas (si la plantilla tuviera)
            for (XWPFTable tbl : document.getTables()) {
                for (XWPFTableRow row : tbl.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph p : cell.getParagraphs()) {
                            replaceInParagraph(p, variables);
                        }
                    }
                }
            }

            // 4. Sección DPE al final del documento
            appendDpeSection(document, context.getDpeSectionLines());

            // 5. Convertir a bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.write(outputStream);

            log.info("Documento Word generado exitosamente en memoria.");
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error generando el archivo Word", e);
            throw ClientException.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .error("No se pudo generar el documento Word.");
        }
    }

    private void appendDpeSection(XWPFDocument document, java.util.List<String> lines) {
        if (lines == null || lines.isEmpty()) return;

        document.createParagraph().createRun().addBreak(BreakType.PAGE);
        boolean first = true;
        for (String line : lines) {
            XWPFRun run = document.createParagraph().createRun();
            run.setText(line);
            if (first || line.endsWith(":")) run.setBold(true);
            first = false;
        }
    }

    /**
     * Reemplaza los placeholders en un párrafo, uniendo los "runs" para evitar cortes.
     */
    private void replaceInParagraph(XWPFParagraph paragraph, Map<String, String> variables) {
        String paragraphText = paragraph.getText();
        if (paragraphText == null || paragraphText.isEmpty()) return;

        boolean hasPlaceholder = false;
        for (String key : variables.keySet()) {
            if (paragraphText.contains("{{" + key + "}}")) {
                hasPlaceholder = true;
                break;
            }
        }

        if (!hasPlaceholder) return;

        // Lógica para unir runs (soluciona el problema de palabras cortadas en Word)
        StringBuilder fullText = new StringBuilder();
        for (XWPFRun run : paragraph.getRuns()) {
            fullText.append(run.text());
        }

        String newText = fullText.toString();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "No especificado";
            newText = newText.replace(placeholder, value);
        }

        // Borrar todos los runs viejos y crear uno nuevo con el texto reemplazado
        for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }

        XWPFRun newRun = paragraph.createRun();
        newRun.setText(newText);

        // (Opcional) Mantener el estilo original: newRun.setFontFamily("Arial");
    }
}