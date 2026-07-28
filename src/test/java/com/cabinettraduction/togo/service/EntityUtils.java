package com.cabinettraduction.togo.service;

import com.cabinettraduction.togo.model.BaseEntity;

import org.springframework.orm.ObjectRetrievalFailureException;

import java.util.Collection;

/**
 * Utilitaires pour manipuler les entités dans les tests.
 */
public abstract class EntityUtils {

	public static <T extends BaseEntity> T getById(Collection<T> entities, Class<T> entityClass, int entityId)
			throws ObjectRetrievalFailureException {
		for (T entity : entities) {
			if (entity.getId() != null && entity.getId() == entityId && entityClass.isInstance(entity)) {
				return entity;
			}
		}
		throw new ObjectRetrievalFailureException(entityClass, entityId);
	}

}
