package org.activiti.cloud.services.query.batch;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public class DeletionHelper {

    private final EntityManager entityManager;

    public DeletionHelper(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public int deleteCascade(Class<?> parentClass, Object parentId) {
        int deleteCount = 0;
        Field idField = getIdField(parentClass);
        if (idField != null) {
            List<Field> oneToManyFields = getOneToManyFields(parentClass);
            for (Field field : oneToManyFields) {
                Class<Object> childClass = getFirstActualTypeArgument(field);
                if (childClass != null) {
                    Field manyToOneField = getManyToOneField(childClass,
                                                             parentClass);
                    Field childClassIdField = getIdField(childClass);
                    if (manyToOneField != null && childClassIdField != null) {
                        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
                        CriteriaQuery<Object> query = builder.createQuery(Object.class);
                        Root<?> root = query.from(childClass);

                        query.select(root.get(childClassIdField.getName()));
                        query.where(root.get(manyToOneField.getName()).get(idField.getName()).in(parentId));

                        List<Object> childIds = entityManager.createQuery(query)
                                                             .getResultList();
                        for (Object childId : childIds) {
                            deleteCount += deleteCascade(childClass, childId);
                        }
                    }
                }
            }
            deleteCount += entityManager.createQuery(String.format("delete from %s e where e.%s = :id",
                                                                   getEntityName(parentClass),
                                                                   idField.getName()))
                                        .setParameter("id", parentId)
                                        .executeUpdate();
        }

        return deleteCount;
    }

    public int delete(Class<Object> parentClass, Object parentId) {
        int deleteCount = 0;
        Field idField = getIdField(parentClass);
        if (idField != null) {
            List<Field> oneToManyFields = getOneToManyFields(parentClass);
            for (Field field : oneToManyFields) {
                Class<Object> childClass = getFirstActualTypeArgument(field);
                if (childClass != null) {
                    Field manyToOneField = getManyToOneField(childClass,
                                                             parentClass);
                    Field childClassIdField = getIdField(childClass);
                    if (manyToOneField != null && childClassIdField != null) {
                        List<Object> childIds = entityManager.createQuery(String.format("select c.%s from %s c where c.%s.%s = :pid",
                                                                                        childClassIdField.getName(),
                                                                                        getEntityName(childClass),
                                                                                        manyToOneField.getName(),
                                                                                        idField.getName()))
                                                             .setParameter("pid", parentId)
                                                             .getResultList();
                        for (Object childId : childIds) {
                            deleteCount += delete(childClass, childId);
                        }
                    }
                }
            }
            deleteCount += entityManager.createQuery(String.format("delete from %s e where e.%s = :id",
                                                                   getEntityName(parentClass),
                                                                   idField.getName()))
                                        .setParameter("id", parentId)
                                        .executeUpdate();
        }

        return deleteCount;
    }

    private String getEntityName(Class<?> childClass) {
        String entityName = childClass.getAnnotation(Entity.class)
                                      .name();

        return entityName.length() > 0 ? entityName
                                       : childClass.getSimpleName();
    }

    private Class<Object> getFirstActualTypeArgument(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (typeArguments.length > 0) {
                Class<Object> childClass = (Class<Object>) typeArguments[0];
                return childClass;
            }
        }
        return null;
    }

    private Field getManyToOneField(Class<?> clazz, Class<?> parentClass) {
        List<Field> declaredFields = getFields(clazz);

        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(ManyToOne.class)) {
                Class<?> type = field.getType();
                if (parentClass.equals(type)) {
                    return field;
                }
            }
        }
        return null;
    }

    private Field getIdField(Class<?> clazz) {
        List<Field> declaredFields = getFields(clazz);

        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }

        return null;
    }

    private List<Field> getOneToManyFields(Class<?> clazz) {
        List<Field> fields = new LinkedList<Field>();

        List<Field> declaredFields = getFields(clazz);
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(OneToMany.class)) {
                fields.add(field);
            }
        }

        return fields;
    }

    private List<Field> getFields(Class<?> clazz) {
        if (clazz == null) {
            return Collections.emptyList();
        }

        List<Field> fields = new ArrayList<>(getFields(clazz.getSuperclass()));

        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));

        return fields;
    }
}