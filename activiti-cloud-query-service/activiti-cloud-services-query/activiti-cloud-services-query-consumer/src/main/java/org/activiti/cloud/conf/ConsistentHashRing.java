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

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class ConsistentHashRing<T> {

    private static final int DEFAULT_VIRTUAL_NODES_PER_NODE = 1;
    private final SortedMap<Long, T> ring = new ConcurrentSkipListMap<>();
    private final int virtualNodesPerNode;

    public ConsistentHashRing() {
        this(DEFAULT_VIRTUAL_NODES_PER_NODE);
    }

    public ConsistentHashRing(int virtualNodesPerNode) {
        if (virtualNodesPerNode <= 0) {
            throw new IllegalArgumentException("virtualNodesPerNode must be greater than zero");
        }
        this.virtualNodesPerNode = virtualNodesPerNode;
    }

    public void addNode(T node) {
        String nodeAddress = Objects.requireNonNull(node, "node cannot be null").toString();
        for (int replica = 0; replica < virtualNodesPerNode; replica++) {
            ring.put(hash(nodeAddress + "#" + replica), node);
        }
    }

    public void removeNode(T node) {
        String nodeAddress = Objects.requireNonNull(node, "node cannot be null").toString();
        for (int replica = 0; replica < virtualNodesPerNode; replica++) {
            ring.remove(hash(nodeAddress + "#" + replica));
        }
    }

    public T getNode(String key) {
        if (ring.isEmpty()) {
            return null;
        }
        long hash = hash(key);

        // Get the tail map of nodes greater than or equal to the key hash
        SortedMap<Long, T> tailMap = ring.tailMap(hash);

        // If there is no tail, wrap around to the first node in the ring
        long nodeHash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();

        return ring.get(nodeHash);
    }

    private long hash(String value) {
        byte[] bytes = Objects.requireNonNull(value, "value cannot be null").getBytes(StandardCharsets.UTF_8);
        int hash = 0x811c9dc5;
        for (byte currentByte : bytes) {
            hash ^= currentByte & 0xff;
            hash *= 0x01000193;
        }
        return Integer.toUnsignedLong(hash);
    }
}
