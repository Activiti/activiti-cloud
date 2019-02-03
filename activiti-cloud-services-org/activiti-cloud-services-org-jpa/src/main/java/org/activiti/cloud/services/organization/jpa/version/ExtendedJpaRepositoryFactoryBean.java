/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.organization.jpa.version;

import java.io.Serializable;
import javax.persistence.EntityManager;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * Bean factory for ExtendedJpaRepositoryFactory
 * @param <R> the repository type
 * @param <T> The entity type
 * @param <K> the entity id type
 * @param <V> the entity version type
 */
public class ExtendedJpaRepositoryFactoryBean<R extends JpaRepository<T, K>, T, K extends Serializable, V extends VersionEntity>
        extends JpaRepositoryFactoryBean<R, T, K> {

    /**
     * Creates a new {@link JpaRepositoryFactoryBean} for the given repository interface.
     * @param repositoryInterface must not be {@literal null}.
     */
    public ExtendedJpaRepositoryFactoryBean(Class<? extends R> repositoryInterface) {
        super(repositoryInterface);
    }

    /**
     * Create a {@link ExtendedJpaRepositoryFactory} instance with the given {@link EntityManager}
     * @param entityManager the entity manager
     * @return the repository factory
     */
    @Override
    protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
        return new ExtendedJpaRepositoryFactory(entityManager);
    }

    /**
     * Factory for JpaRepository and VersionedJpaRepository
     * @param <T> The entity type
     * @param <ID> the entity id type
     * @param <V> the entity version type
     */
    private static class ExtendedJpaRepositoryFactory<T extends VersionedEntity, ID extends Serializable, V extends VersionEntity>
            extends JpaRepositoryFactory {

        public ExtendedJpaRepositoryFactory(EntityManager em) {
            super(em);
        }

        /**
         * Create a {@link JpaRepository} instance with the given {@link EntityManager}
         * @param entityManager the entity manager
         * @return the repository implementation
         */
        @Override
        protected JpaRepositoryImplementation getTargetRepository(final RepositoryInformation information,
                                                                  final EntityManager entityManager) {
            if (isVersionedJpaRepository(information.getRepositoryInterface())) {
                return getTargetVersionedJpaRepository(information.getRepositoryInterface(),
                                                       entityManager);
            }

            return super.getTargetRepository(information,
                                             entityManager);
        }

        /**
         * Check if a repository interface is a version repository.
         * @param repositoryInterface the repository interface
         * @return true if the given repository interface is a version one
         */
        protected boolean isVersionedJpaRepository(Class<?> repositoryInterface) {
            return VersionedJpaRepository.class.isAssignableFrom(repositoryInterface);
        }

        /**
         * Create a {@link VersionedJpaRepositoryImpl} instance with the given {@link EntityManager}.
         * @param repositoryInterface the repository interface
         * @param entityManager the entity manager
         * @return the repository implementation
         */
        protected VersionedJpaRepositoryImpl getTargetVersionedJpaRepository(Class<?> repositoryInterface,
                                                                             EntityManager entityManager) {
            VersionedRepositoryMetadata metadata = new VersionedRepositoryMetadata(repositoryInterface);

            Class<T> versionedEntityType = (Class<T>) metadata.getDomainType();
            Class<V> versionEntityType = (Class<V>) metadata.getVersionEntityType();
            return new VersionedJpaRepositoryImpl<T, ID, V>(versionedEntityType,
                                                            versionEntityType,
                                                            entityManager);
        }
    }
}
