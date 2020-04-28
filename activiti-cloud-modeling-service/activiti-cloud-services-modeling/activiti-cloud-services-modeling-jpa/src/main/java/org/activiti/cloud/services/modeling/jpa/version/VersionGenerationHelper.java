package org.activiti.cloud.services.modeling.jpa.version;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import org.springframework.dao.DataIntegrityViolationException;

public class VersionGenerationHelper<T extends VersionedEntity,V extends VersionEntity> {

    private final VersionGenerator versionGenerator = new VersionGenerator();

    private Class<T> versionedClass;

    private Class<V> versionClass;

    public VersionGenerationHelper(final Class<T> versionedClass,
        final Class<V> versionClass){

        this.versionedClass = versionedClass;
        this.versionClass = versionClass;
    }

    /**
     * Generate and add a new version to a given version entity.
     * @param versionedEntity the version entity to generate for
     */
    public void generateNextVersion(T versionedEntity) {

        String nextVersion = versionGenerator
            .generateNextVersion(versionedEntity.getLatestVersion());

        try {
            V newVersion = versionClass.getDeclaredConstructor(versionClass).newInstance(versionedEntity.getLatestVersion());
            newVersion.setVersionedEntity(versionedEntity);
            newVersion.setVersionIdentifier(new VersionIdentifier(versionedEntity.getId(),
                nextVersion));

            if (versionedEntity.getVersions() == null) {
                versionedEntity.setVersions(new ArrayList<>());
            }
            versionedEntity.getVersions().add(newVersion);
            versionedEntity.setLatestVersion(newVersion);
        } catch (NoSuchMethodException | InvocationTargetException e) {
            throw new DataIntegrityViolationException(
                String.format("Invalid version class %s: No copy constructor declared",
                    versionClass),
                e);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new DataIntegrityViolationException(
                String.format("Cannot add a new version of type %s for version entity type %s",
                    versionClass,
                    versionedClass),
                e);
        }
    }
}
