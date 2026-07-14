package com.asdf.tongchoobe.dto.request;

import com.asdf.tongchoobe.domain.Target;
import com.asdf.tongchoobe.domain.Tone;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcuseCreateRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void customTargetRequiresNaturalLanguageDescription() throws Exception {
        Set<ConstraintViolation<ExcuseCreateRequest>> violations = validator.validate(
                request(Target.CUSTOM, null)
        );

        assertTrue(hasTargetDescriptionViolation(violations));
    }

    @Test
    void customTargetAcceptsNaturalLanguageDescriptionUpTo100Characters() throws Exception {
        Set<ConstraintViolation<ExcuseCreateRequest>> violations = validator.validate(
                request(Target.CUSTOM, "회사 부장님")
        );

        assertTrue(violations.isEmpty());
    }

    @Test
    void standardTargetRejectsTargetDescription() throws Exception {
        Set<ConstraintViolation<ExcuseCreateRequest>> violations = validator.validate(
                request(Target.TEAM_LEAD, "회사 부장님")
        );

        assertTrue(hasTargetDescriptionViolation(violations));
    }

    @Test
    void standardTargetWithoutDescriptionKeepsExistingRequestContract() throws Exception {
        Set<ConstraintViolation<ExcuseCreateRequest>> violations = validator.validate(
                request(Target.FRIEND, null)
        );

        assertFalse(hasTargetDescriptionViolation(violations));
        assertTrue(violations.isEmpty());
    }

    @Test
    void descriptionOver100CharactersIsRejected() throws Exception {
        Set<ConstraintViolation<ExcuseCreateRequest>> violations = validator.validate(
                request(Target.CUSTOM, "가".repeat(101))
        );

        assertTrue(hasTargetDescriptionViolation(violations));
    }

    private boolean hasTargetDescriptionViolation(Set<ConstraintViolation<ExcuseCreateRequest>> violations) {
        return violations.stream()
                .anyMatch(violation -> "targetDescription".equals(violation.getPropertyPath().toString()));
    }

    private ExcuseCreateRequest request(Target target, String targetDescription) throws Exception {
        ExcuseCreateRequest request = new ExcuseCreateRequest();
        set(request, "situation", "회식 참석이 어렵습니다.");
        set(request, "target", target);
        set(request, "targetDescription", targetDescription);
        set(request, "tone", Tone.MILD);
        return request;
    }

    private void set(ExcuseCreateRequest request, String fieldName, Object value) throws Exception {
        Field field = ExcuseCreateRequest.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(request, value);
    }
}
