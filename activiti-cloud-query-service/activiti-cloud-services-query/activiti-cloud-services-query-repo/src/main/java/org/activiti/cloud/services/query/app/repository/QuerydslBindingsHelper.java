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
package org.activiti.cloud.services.query.app.repository;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.EntityPathBase;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.data.querydsl.binding.QuerydslBindings;

public class QuerydslBindingsHelper {

    private final EntityPathBase<?> root;
    private final Set<Path<?>> excluding = new LinkedHashSet<>();

    public QuerydslBindingsHelper(EntityPathBase<?> root) {
        this.root = root;
    }

    public static QuerydslBindingsHelper whitelist(EntityPathBase<?> root) {
        return new QuerydslBindingsHelper(root);
    }

    public QuerydslBindingsHelper excluding(Path<?>... paths) {
        excluding.addAll(Arrays.asList(paths));

        return this;
    }

    public void apply(QuerydslBindings bindings) {
        Arrays
            .stream(root.getClass().getDeclaredFields())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .filter(field -> !EntityPath.class.isAssignableFrom(field.getType()))
            .filter(field -> Path.class.isAssignableFrom(field.getType()))
            .sorted(Comparator.comparing(Field::getName))
            .map(field -> {
                try {
                    return field.get(root);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .map(Path.class::cast)
            .filter(path -> !excluding.contains(path))
            .forEach(bindings::including);

        if (!excluding.isEmpty()) {
            bindings.excluding(excluding.toArray(Path[]::new));
        }

        bindings.excludeUnlistedProperties(true);
    }
}
