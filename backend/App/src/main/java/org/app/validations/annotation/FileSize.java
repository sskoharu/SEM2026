package org.app.validations.annotation;

import org.app.validations.validator.FileSizeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FileSizeValidator.class)
@Documented

public @interface FileSize {
    String message() default "Archivo demasiado grande";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    long max(); // tamaño máximo
    Unit unit() default Unit.MB;

    enum Unit {
        KB, MB
    }
}
