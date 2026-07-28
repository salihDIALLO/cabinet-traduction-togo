package com.cabinettraduction.togo.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

class ValidatorTests {

	private Validator createValidator() {
		LocalValidatorFactoryBean localValidatorFactoryBean = new LocalValidatorFactoryBean();
		localValidatorFactoryBean.afterPropertiesSet();
		return localValidatorFactoryBean;
	}

	private <T> ConstraintViolation<T> getOnlyViolation(Set<ConstraintViolation<T>> violations) {
		assertThat(violations).hasSize(1);
		return violations.iterator().next();
	}

	@Test
	void shouldNotValidateWhenFirstNameEmpty() {
		LocaleContextHolder.setLocale(Locale.ENGLISH);

		Person person = new Person();
		person.setFirstName("");
		person.setLastName("smith");

		Validator validator = createValidator();
		Set<ConstraintViolation<Person>> constraintViolations = validator.validate(person);

		ConstraintViolation<Person> violation = getOnlyViolation(constraintViolations);
		assertThat(violation.getPropertyPath()).hasToString("firstName");
		assertThat(violation.getMessage()).isEqualTo("must not be blank");
	}

	@Test
	void shouldNotValidateWhenLastNameEmpty() {
		LocaleContextHolder.setLocale(Locale.ENGLISH);

		Person person = new Person();
		person.setFirstName("George");
		person.setLastName("");

		Validator validator = createValidator();
		Set<ConstraintViolation<Person>> constraintViolations = validator.validate(person);

		ConstraintViolation<Person> violation = getOnlyViolation(constraintViolations);
		assertThat(violation.getPropertyPath()).hasToString("lastName");
		assertThat(violation.getMessage()).isEqualTo("must not be blank");
	}

}
