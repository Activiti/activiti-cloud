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

import java.util.List;
import java.util.function.Supplier;

import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * RepositoryMetadata with versioning support
 */
public class VersionedRepositoryMetadata extends DefaultRepositoryMetadata {

    private Class<? extends VersionEntity> versionEntityType;

    /**
     * Creates a new {@link DefaultRepositoryMetadata} for the given repository interface.
     * @param repositoryInterface the repository interface.
     */
    public VersionedRepositoryMetadata(Class<?> repositoryInterface) {
        super(repositoryInterface);

        Class<?> type = resolveTypeParameter(
                repositoryInterface,
                2,
                () -> String.format("Could not resolve version entity type of %s",
                                    repositoryInterface));

        checkTypeParameter(getDomainType(),
                           VersionedEntity.class,
                           () -> String.format("The specified version entity type %s is not subtype of VersionedEntity for repository %s",
                                               getDomainType(),
                                               repositoryInterface));
        checkTypeParameter(type,
                           VersionEntity.class,
                           () -> String.format("The specified version entity type %s is not subtype of VersionEntity for repository %s",
                                               type,
                                               repositoryInterface));

        this.versionEntityType = (Class<? extends VersionEntity>) type;
    }

    public Class<? extends VersionEntity> getVersionEntityType() {
        return versionEntityType;
    }

    public void setVersionEntityType(Class<? extends VersionEntity> versionEntityType) {
        this.versionEntityType = versionEntityType;
    }

    /**
     * Resolve the type at a given index for the repository interface.
     *
     * @param repositoryInterface the repository interface
     * @param index the index of the type
     * @param exceptionMessage the exception message to throw if there is no type a given index
     * @return the type
     */
    private static Class<?> resolveTypeParameter(Class<?> repositoryInterface,
                                                 int index,
                                                 Supplier<String> exceptionMessage) {

        List<TypeInformation<?>> arguments = ClassTypeInformation
                .from(repositoryInterface)
                .getRequiredSuperTypeInformation(VersionedJpaRepository.class)
                .getTypeArguments();

        if (arguments.size() <= index || arguments.get(index) == null) {
            throw new IllegalArgumentException(exceptionMessage.get());
        }

        return arguments.get(index).getType();
    }

    /**
     * Check if a given type of the expected type.
     *
     * @param typeToCheck the type to check
     * @param expectedType the expected type
     * @param exceptionMessage the exception message to throw if the type is not the expected one
     */
    private void checkTypeParameter(Class<?> typeToCheck,
                                    Class<?> expectedType,
                                    Supplier<String> exceptionMessage) {

        if (!(expectedType.isAssignableFrom(typeToCheck))) {
            throw new IllegalArgumentException(exceptionMessage.get());
        }
    }

}
