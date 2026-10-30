package org.app.services.context.layer_1;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.app.schemas.session_object.SessionObject;
import org.springframework.ai.document.Document;

import java.util.List;

@Data
@AllArgsConstructor
public class Layer1ExecutionContext {
    private SessionObject session;
    private List<Document> retrievedDocs; // Documentos recuperados en memoria
}