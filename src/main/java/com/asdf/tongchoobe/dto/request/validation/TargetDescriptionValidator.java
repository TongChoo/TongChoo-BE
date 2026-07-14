package com.asdf.tongchoobe.dto.request.validation;

import com.asdf.tongchoobe.domain.Target;
import com.asdf.tongchoobe.dto.request.ExcuseCreateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** target과 targetDescription의 조합을 targetDescription 필드 오류로 돌려준다. */
public class TargetDescriptionValidator implements ConstraintValidator<ValidTargetDescription, ExcuseCreateRequest> {

    @Override
    public boolean isValid(ExcuseCreateRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getTarget() == null) {
            return true;
        }

        String description = request.getTargetDescription();
        boolean valid = request.getTarget() == Target.CUSTOM
                ? description != null && !description.trim().isEmpty()
                : description == null;
        if (valid) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        String message = request.getTarget() == Target.CUSTOM
                ? "직접 입력 관계를 1~100자로 입력해주세요."
                : "직접 입력 관계는 CUSTOM 대상에서만 사용할 수 있습니다.";
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("targetDescription")
                .addConstraintViolation();
        return false;
    }
}
