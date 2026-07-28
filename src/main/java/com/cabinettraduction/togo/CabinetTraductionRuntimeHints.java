package com.cabinettraduction.togo;

import com.cabinettraduction.togo.model.BaseEntity;
import com.cabinettraduction.togo.model.Person;
import com.cabinettraduction.togo.vet.Vet;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class CabinetTraductionRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		hints.resources().registerPattern("db/*");
		hints.resources().registerPattern("db/*/*");
		hints.resources().registerPattern("messages/*");
		hints.resources().registerPattern("mysql-default-conf");
		hints.reflection().registerType(BaseEntity.class, typeHint -> typeHint.withJavaSerialization(true));
		hints.reflection().registerType(Person.class, typeHint -> typeHint.withJavaSerialization(true));
		hints.reflection().registerType(Vet.class, typeHint -> typeHint.withJavaSerialization(true));
	}

}
