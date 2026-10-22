package org.app.validations.validator;

import org.app.validations.annotation.FileType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class FileTypeValidator implements ConstraintValidator<FileType, MultipartFile> {
    private Set<String> allowedExtensions;

    @Override
    public void initialize(FileType constraintAnnotation) {
        this.allowedExtensions = Arrays.stream(constraintAnnotation.value())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) return false;

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) return false;

        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return allowedExtensions.contains(extension);
    }

}
