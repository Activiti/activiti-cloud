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
package org.activiti.cloud.notifications.graphql.starter;

import java.util.LinkedHashMap;
import java.util.Map;

class MapBuilder<B extends MapBuilder<B, K, V>, K, V> {

    private final Map<K, V> map = new LinkedHashMap<>();

    public B put(K key, V value) {
        this.map.put(key, value);
        return _this();
    }

    public Map<K, V> get() {
        return this.map;
    }

    @SuppressWarnings("unchecked")
    protected final B _this() {
        return (B) this;
    }
}
