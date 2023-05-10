/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.modeling.jpa.version;

import java.io.Serializable;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation for {@link VersionedJpaRepository}
 */
public class VersionedJpaRepositoryImpl<T extends VersionedEntity, K extends Serializable, V extends VersionEntity>
    extends SimpleJpaRepository<T, K>
    implements VersionedJpaRepository<T, K, V> {

    private final VersionGenerationHelper<T, V> versionGenerationHelper;

    /**
     * Creates a new {@link SimpleJpaRepository} to manage objects of the given domain type.
     *
     * @param versionedClass the class of the version entity.
     * @param versionClass   the class of the version entity.
     * @param entityManager  must not be {@literal null}.
     */
    public VersionedJpaRepositoryImpl(
        final Class<T> versionedClass,
        final Class<V> versionClass,
        final EntityManager entityManager
    ) {
        super(versionedClass, entityManager);
        this.versionGenerationHelper = new VersionGenerationHelper<T, V>(versionedClass, versionClass);
    }

    /**
     * Add a new version before any save.
     *
     * @param versionedEntity the entity to save
     * @param <S>             the versionedEntity type
     * @return the saved entity
     */

    @Override
    @Transactional
    public <S extends T> S save(S versionedEntity) {
        this.versionGenerationHelper.generateNextVersion(versionedEntity);

        return super.save(versionedEntity);
    }
}
