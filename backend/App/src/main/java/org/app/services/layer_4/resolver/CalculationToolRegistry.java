package org.app.services.layer_4.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.app.services.layer_4.tools.CalculationTools;
import org.app.services.layer_4.tools.common.AmountCalculationTools;
import org.app.services.layer_4.tools.common.DateCalculationTools;
import org.app.services.layer_4.tools.type_1_vicios_ocultos.ViciosOcultosCalculationTools;
import org.app.services.layer_4.tools.type_2_servicio_tecnico.ServicioTecnicoCalculationTools;
import org.app.services.layer_4.tools.type_4_informacion_facturacion.InformacionFacturacionCalculationTools;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Ejecuta por nombre cualquier método @Tool de la Capa 4, validando que estén todos los argumentos. */
@Component
public class CalculationToolRegistry {

    private record Registered(Object bean, Method method) {
    }

    private final Map<String, Registered> tools = new HashMap<>();
    private final ObjectMapper mapper;

    public CalculationToolRegistry(ObjectMapper mapper,
                                   CalculationTools legacy,
                                   DateCalculationTools dates,
                                   AmountCalculationTools amounts,
                                   ViciosOcultosCalculationTools vicios,
                                   ServicioTecnicoCalculationTools servicio,
                                   InformacionFacturacionCalculationTools info) {
        this.mapper = mapper;
        for (Object bean : List.of(legacy, dates, amounts, vicios, servicio, info)) {
            for (Method m : bean.getClass().getMethods()) {
                if (m.isAnnotationPresent(Tool.class)) {
                    tools.put(m.getName(), new Registered(bean, m));
                }
            }
        }
    }

    public boolean exists(String toolName) {
        return tools.containsKey(toolName);
    }

    public Object invoke(String toolName, Map<String, Object> args) {
        Registered tool = tools.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Herramienta desconocida: " + toolName);
        }
        Parameter[] params = tool.method().getParameters();
        Object[] values = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Object raw = args.get(params[i].getName());
            if (raw == null) {
                throw new IllegalArgumentException("Falta el argumento '" + params[i].getName() + "' de " + toolName);
            }
            values[i] = mapper.convertValue(raw, params[i].getType());
        }
        try {
            return tool.method().invoke(tool.bean(), values);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IllegalStateException(cause.getMessage(), cause);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
