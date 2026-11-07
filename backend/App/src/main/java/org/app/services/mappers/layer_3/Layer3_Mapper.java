package org.app.services.mappers.layer_3;

import org.app.schemas.session_object.SessionObject;
import org.app.services.layer_3.dto.Layer3ActionResponseDto;
import org.app.services.layer_3.dto.ProceduralOption;
import org.springframework.stereotype.Component;

@Component
public class Layer3_Mapper {

    // 1. CORRECCIÓN: El método debe ser público, devolver el DTO y no la clase Mapper
    public Layer3ActionResponseDto mapToDto(SessionObject session) {

        if (session == null) {
            return null;
        }

        // 2. CORRECCIÓN: El builder se declara como Layer3ActionResponseDtoBuilder
        Layer3ActionResponseDto.Layer3ActionResponseDtoBuilder builder = Layer3ActionResponseDto.builder()
                .sessionId(session.getSessionId())
                .layer3State(session.getLayer3State() != null ? session.getLayer3State().name() : null)
                .viabilityReport(session.getViabilityReport())
                .clarificationMessage(session.getClarificationMessage())
                .selectedOptionId(session.getSelectedOptionId())
                .executionPlan(session.getExecutionPlan());
        // 3. CORRECCIÓN: No ponemos .build() aquí para poder seguir agregando datos abajo

        // Si hay una opción seleccionada, extraemos solo su información
        if (session.getActionRouteOptions() != null && session.getSelectedOptionId() != null) {
            ProceduralOption selectedOption = session.getActionRouteOptions().getOptions().stream()
                    .filter(opt -> opt.getOptionId().equals(session.getSelectedOptionId()))
                    .findFirst()
                    .orElse(null);

            if (selectedOption != null) {
                builder.selectedOptionTitle(selectedOption.getTitle())
                        .selectedOptionDescription(selectedOption.getDescription())
                        .formInputsRequired(selectedOption.getUserInputsRequired());
            }
        }

        // 4. Aquí sí, construimos y devolvemos el objeto final
        return builder.build();
    }
}