package org.app.schemas.interfaces.layer_1;

import org.app.services.context.layer_1.Layer1ExecutionContext;
import org.app.schemas.session_object.enums.Layer1InternalState;

public interface Layer1Step {
    Layer1InternalState getState();
    Layer1InternalState execute(Layer1ExecutionContext context);
}