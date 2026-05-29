package sdu.ai.lab.authservice.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {FilesValidator.class})
public @interface ValidFiles {
    String message() default "{error.upload.invalidFileFormat}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

