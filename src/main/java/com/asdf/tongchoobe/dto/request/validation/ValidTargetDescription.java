package com.asdf.tongchoobe.dto.request.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** CUSTOM 대상일 때만 자연어 관계 설명을 허용하는 클래스 단위 검증이다. */
@Documented
@Constraint(validatedBy = TargetDescriptionValidator.class)
@java.lang.annotation.Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTargetDescription {
    String message() default "직접 입력 대상에는 관계 설명이 필요합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
