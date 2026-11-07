package org.app.services.layer_2.components.c0.extractor_agents;

import org.app.exceptions.ClientException;
import org.springframework.http.HttpStatus;
import org.app.schemas.interfaces.layer_2.ILayer2ExtractionAgent;
import org.app.schemas.session_object.enums.ClaimType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ExtractionAgentFactory {

    private final Map<String, ILayer2ExtractionAgent> agents;

    public ExtractionAgentFactory(List<ILayer2ExtractionAgent> agentList) {
        this.agents = agentList.stream()
                .collect(Collectors.toMap(ILayer2ExtractionAgent::getSupportedClaimType, Function.identity()));
    }

    public ILayer2ExtractionAgent getAgent(ClaimType claimType) {
        ILayer2ExtractionAgent agent = agents.get(claimType.name());
        if (agent == null) {
            throw ClientException.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .error("No existe un agente de extracción para el tipo: " + claimType.name());
        }
        return agent;
    }
}
