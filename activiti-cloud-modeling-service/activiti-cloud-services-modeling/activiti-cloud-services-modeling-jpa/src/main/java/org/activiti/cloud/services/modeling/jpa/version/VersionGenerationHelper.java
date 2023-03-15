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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import org.springframework.dao.DataIntegrityViolationException;

public class VersionGenerationHelper<T extends VersionedEntity, V extends VersionEntity> {

    private final VersionGenerator versionGenerator = new VersionGenerator();

    private Class<T> versionedClass;

    private Class<V> versionClass;

    public VersionGenerationHelper(final Class<T> versionedClass, final Class<V> versionClass) {
        this.versionedClass = versionedClass;
        this.versionClass = versionClass;
    }

    /**
     * Generate and add a new version to a given version entity.
     * @param versionedEntity the version entity to generate for
     */
    public void generateNextVersion(T versionedEntity) {
        String nextVersion = versionGenerator.generateNextVersion(versionedEntity.getLatestVersion());

        try {
            V newVersion = versionClass
                .getDeclaredConstructor(versionClass)
                .newInstance(versionedEntity.getLatestVersion());
            newVersion.setVersionedEntity(versionedEntity);
            newVersion.setVersionIdentifier(new VersionIdentifier(versionedEntity.getId(), nextVersion));

            if (versionedEntity.getVersions() == null) {
                versionedEntity.setVersions(new ArrayList<>());
            }
            versionedEntity.getVersions().add(newVersion);
            versionedEntity.setLatestVersion(newVersion);
        } catch (NoSuchMethodException | InvocationTargetException e) {
            throw new DataIntegrityViolationException(
                String.format("Invalid version class %s: No copy constructor declared", versionClass),
                e
            );
        } catch (InstantiationException | IllegalAccessException e) {
            throw new DataIntegrityViolationException(
                String.format(
                    "Cannot add a new version of type %s for version entity type %s",
                    versionClass,
                    versionedClass
                ),
                e
            );
        }
    }
}
