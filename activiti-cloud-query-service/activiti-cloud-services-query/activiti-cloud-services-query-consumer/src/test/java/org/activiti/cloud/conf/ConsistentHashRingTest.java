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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConsistentHashRingTest {

    private ConsistentHashRing<String> stringRing;

    @BeforeEach
    void setUp() {
        stringRing = new ConsistentHashRing<>();
    }

    @Nested
    @DisplayName("Empty Ring Tests")
    class EmptyRingTests {

        @Test
        @DisplayName("Should return null when getting a node from an empty ring")
        void shouldReturnNullWhenRingIsEmpty() {
            String key = "test-key";

            String node = stringRing.getNode(key);

            assertThat(node).isNull();
        }
    }

    @Nested
    @DisplayName("Single Node Tests")
    class SingleNodeTests {

        @BeforeEach
        void addSingleNode() {
            stringRing.addNode("Node-A");
        }

        @Test
        @DisplayName("Should always return the same node regardless of the key")
        void shouldAlwaysReturnSingleNode() {
            assertThat(stringRing.getNode("key-1")).isEqualTo("Node-A");
            assertThat(stringRing.getNode("key-2")).isEqualTo("Node-A");
            assertThat(stringRing.getNode("very-long-key-with-different-hash")).isEqualTo("Node-A");
        }

        @Test
        @DisplayName("Should return null if the single node is removed")
        void shouldReturnNullAfterRemovingOnlyNode() {
            stringRing.removeNode("Node-A");

            assertThat(stringRing.getNode("any-key")).isNull();
        }
    }

    @Nested
    @DisplayName("Multi-Node and Routing Tests")
    class MultiNodeTests {

        @BeforeEach
        void addMultipleNodes() {
            stringRing.addNode("Node-A");
            stringRing.addNode("Node-B");
            stringRing.addNode("Node-C");
        }

        @Test
        @DisplayName("Should route deterministically to the same node for the same key")
        void shouldRouteDeterministically() {
            String key = "my-stable-key";

            String firstLookup = stringRing.getNode(key);
            String secondLookup = stringRing.getNode(key);
            String thirdLookup = stringRing.getNode(key);

            assertThat(firstLookup).isNotNull().isEqualTo(secondLookup).isEqualTo(thirdLookup);
        }

        @Test
        @DisplayName("Should always resolve a known node for any key")
        void shouldAlwaysResolveKnownNode() {
            assertThat(stringRing.getNode("wrap-around-test-key")).isIn("Node-A", "Node-B", "Node-C");
            assertThat(stringRing.getNode("another-key")).isIn("Node-A", "Node-B", "Node-C");
        }

        @Test
        @DisplayName("Should re-route keys to remaining nodes when a node is removed")
        void shouldReRouteWhenNodeIsRemoved() {
            String key = "key-tied-to-node-b";
            stringRing.addNode("Node-B");

            String initialNode = stringRing.getNode(key);

            // Remove the node that the key was routed to
            stringRing.removeNode(initialNode);

            String newNode = stringRing.getNode(key);

            assertThat(newNode).isNotNull().isNotEqualTo(initialNode).isIn("Node-A", "Node-B", "Node-C");
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should keep routing unchanged when removing non-existent node")
        void shouldKeepRoutingWhenRemovingNonExistentNode() {
            stringRing.addNode("Node-A");

            stringRing.removeNode("Node-B");

            assertThat(stringRing.getNode("any-key")).isEqualTo("Node-A");
        }

        @Test
        @DisplayName("Should overwrite node mapping when a duplicate hash is added")
        void shouldOverwriteNodeMappingWhenDuplicateHashIsAdded() {
            class CollisionNode {

                @Override
                public String toString() {
                    return "collision-key";
                }
            }

            ConsistentHashRing<CollisionNode> ring = new ConsistentHashRing<>();
            CollisionNode firstNode = new CollisionNode();
            CollisionNode secondNode = new CollisionNode();
            ring.addNode(firstNode);
            ring.addNode(secondNode);

            assertThat(ring.getNode("any-key")).isSameAs(secondNode);
        }

        @Test
        @DisplayName("Should throw NullPointerException when adding null node")
        void shouldThrowNullPointerWhenAddingNullNode() {
            assertThatThrownBy(() -> stringRing.addNode(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw NullPointerException when removing null node")
        void shouldThrowNullPointerWhenRemovingNullNode() {
            assertThatThrownBy(() -> stringRing.removeNode(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw NullPointerException when key is null and ring is not empty")
        void shouldThrowNullPointerWhenKeyIsNullAndRingIsNotEmpty() {
            stringRing.addNode("Node-A");

            assertThatThrownBy(() -> stringRing.getNode(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Generic Type Tests")
    class GenericTypeTests {

        @Test
        @DisplayName("Should support Integer nodes natively")
        void shouldSupportIntegerNodes() {
            ConsistentHashRing<Integer> integerRing = new ConsistentHashRing<>();
            integerRing.addNode(100);
            integerRing.addNode(200);

            Integer assignedNode = integerRing.getNode("user-id-45");

            assertThat(assignedNode).isIn(100, 200);
        }

        @Nested
        @DisplayName("Virtual Node Tests")
        class VirtualNodeTests {

            @Test
            @DisplayName("Should reject non-positive virtual node counts")
            void shouldRejectNonPositiveVirtualNodeCount() {
                assertThatThrownBy(() -> new ConsistentHashRing<String>(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("virtualNodesPerNode");
            }

            @Test
            @DisplayName("Should keep deterministic routing with virtual nodes")
            void shouldKeepDeterministicRoutingWithVirtualNodes() {
                ConsistentHashRing<String> virtualNodeRing = new ConsistentHashRing<>(128);
                virtualNodeRing.addNode("Node-A");
                virtualNodeRing.addNode("Node-B");
                virtualNodeRing.addNode("Node-C");

                String key = UUID.randomUUID().toString();

                assertThat(virtualNodeRing.getNode(key))
                    .isEqualTo(virtualNodeRing.getNode(key))
                    .isEqualTo(virtualNodeRing.getNode(key));
            }

            @Test
            @DisplayName("Should improve UUID distribution when virtual nodes are enabled")
            void shouldImproveUuidDistributionWithVirtualNodes() {
                ConsistentHashRing<Integer> singleNodeRing = new ConsistentHashRing<>(1);
                ConsistentHashRing<Integer> virtualNodeRing = new ConsistentHashRing<>(256);
                for (int node = 0; node < 8; node++) {
                    singleNodeRing.addNode(node);
                    virtualNodeRing.addNode(node);
                }

                Map<Integer, Integer> singleNodeDistribution = distributionFor(singleNodeRing, 20_000);
                Map<Integer, Integer> virtualNodeDistribution = distributionFor(virtualNodeRing, 20_000);

                int singleNodeSkew = skew(singleNodeDistribution);
                int virtualNodeSkew = skew(virtualNodeDistribution);

                assertThat(singleNodeDistribution).hasSize(8);
                assertThat(virtualNodeDistribution).hasSize(8);
                assertThat(virtualNodeSkew).isLessThan(singleNodeSkew);
            }

            private static Map<Integer, Integer> distributionFor(ConsistentHashRing<Integer> ring, int keysCount) {
                Map<Integer, Integer> distribution = new HashMap<>();
                for (int i = 0; i < keysCount; i++) {
                    Integer node = ring.getNode(UUID.randomUUID().toString());
                    distribution.merge(node, 1, Integer::sum);
                }
                return distribution;
            }

            private static int skew(Map<Integer, Integer> distribution) {
                int min = distribution.values().stream().min(Integer::compareTo).orElseThrow();
                int max = distribution.values().stream().max(Integer::compareTo).orElseThrow();
                return max - min;
            }
        }

        @Test
        @DisplayName("Should work cleanly with custom Object nodes via toString")
        void shouldSupportCustomObjectNodes() {
            class CustomNode {

                private final String id;

                CustomNode(String id) {
                    this.id = id;
                }

                @Override
                public String toString() {
                    return id;
                }
            }

            ConsistentHashRing<CustomNode> customRing = new ConsistentHashRing<>();
            CustomNode node1 = new CustomNode("server-1");
            CustomNode node2 = new CustomNode("server-2");

            customRing.addNode(node1);
            customRing.addNode(node2);

            CustomNode assignedNode = customRing.getNode("session-key");

            assertThat(assignedNode).isIn(node1, node2);
        }
    }

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @DisplayName("Should handle high-volume concurrent reads without throwing exceptions")
        void shouldHandleConcurrentReads() throws InterruptedException {
            // Arrange
            stringRing.addNode("Node-A");
            stringRing.addNode("Node-B");
            stringRing.addNode("Node-C");

            int threadCount = 20;
            int lookupsPerThread = 500;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(threadCount);
            AtomicInteger errorCount = new AtomicInteger(0);

            // Act
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Synchronize thread start
                        for (int j = 0; j < lookupsPerThread; j++) {
                            String key = "key-" + UUID.randomUUID().toString();
                            String node = stringRing.getNode(key);
                            if (node == null || !node.startsWith("Node-")) {
                                errorCount.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // Fire all threads simultaneously
            boolean finishedCleanly = completionLatch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            // Assert
            assertThat(finishedCleanly).as("Threads should complete within timeout").isTrue();
            assertThat(errorCount.get()).as("No errors or exceptions should happen during concurrent reads").isZero();
        }

        @Test
        @DisplayName(
            "Should safely handle interleaved reads and writes without throwing ConcurrentModificationException"
        )
        @Disabled
        void shouldHandleConcurrentReadsAndWrites() throws InterruptedException {
            // Arrange
            stringRing.addNode("Node-Base");

            int readerThreadsCount = 10;
            int writerThreadsCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(readerThreadsCount + writerThreadsCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(readerThreadsCount + writerThreadsCount);
            AtomicInteger exceptionCount = new AtomicInteger(0);

            // Act: Writers constantly adding/removing nodes
            for (int i = 0; i < writerThreadsCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < 100; j++) {
                            String nodeName = "Dynamic-Node-" + threadId + "-" + j;
                            stringRing.addNode(nodeName);
                            Thread.yield(); // Give readers a chance to interleave
                            stringRing.removeNode(nodeName);
                        }
                    } catch (Exception e) {
                        exceptionCount.incrementAndGet();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            // Act: Readers constantly scanning the ring
            for (int i = 0; i < readerThreadsCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < 500; j++) {
                            String node = stringRing.getNode("stable-key");
                            // Ring must always resolve to at least the base node, never crash or be null
                            if (node == null) {
                                exceptionCount.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        exceptionCount.incrementAndGet();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // Fire
            boolean finishedCleanly = completionLatch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            // Assert
            assertThat(finishedCleanly).as("Operations should complete within timeframe").isTrue();
            assertThat(exceptionCount.get())
                .as("Should not throw ConcurrentModificationException or lose structural data stability")
                .isZero();
        }
    }
}
