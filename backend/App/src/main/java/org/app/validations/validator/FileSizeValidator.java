package org.app.validations.validator;


import org.app.validations.annotation.FileSize;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

public class FileSizeValidator implements ConstraintValidator<FileSize, MultipartFile> {
    private long maxBytes;

    @Override
    public void initialize(FileSize constraintAnnotation) {
        long multiplier = switch (constraintAnnotation.unit()) {
            case KB -> 1024;
            case MB -> 1024 * 1024;
        };
        this.maxBytes = constraintAnnotation.max() * multiplier;
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        return file != null && file.getSize() <= maxBytes;
    }
}
