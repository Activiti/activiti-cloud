/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.conf;

import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class ConsistentHashRing<T> {

    private final SortedMap<Long, T> ring = new ConcurrentSkipListMap<>();

    public void addNode(T node) {
        String nodeAddress = node.toString();
        long hash = Math.abs((long) nodeAddress.hashCode());
        ring.put(hash, node);
    }

    public void removeNode(T node) {
        long hash = Math.abs((long) node.toString().hashCode());
        ring.remove(hash);
    }

    public T getNode(String key) {
        if (ring.isEmpty()) {
            return null;
        }
        long hash = Math.abs((long) key.hashCode());

        // Get the tail map of nodes greater than or equal to the key hash
        SortedMap<Long, T> tailMap = ring.tailMap(hash);

        // If there is no tail, wrap around to the first node in the ring
        long nodeHash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();

        return ring.get(nodeHash);
    }
}
