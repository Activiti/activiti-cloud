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

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;

public class UUIDConsumerPartitionedChannelKeySelector implements QueryConsumerPartitionedChannelKeySelector {

    private static final Pattern UUID_REGEX = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private final int totalPartitions;

    public UUIDConsumerPartitionedChannelKeySelector(int totalPartitions) {
        if (totalPartitions <= 0) {
            throw new IllegalArgumentException("totalPartitions must be greater than zero");
        }

        this.totalPartitions = totalPartitions;
    }

    @Override
    public Object apply(Message<?> message) {
        return Optional
            .ofNullable(message.getHeaders().get(ROOT_PROCESS_INSTANCE_ID))
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .filter(it -> UUID_REGEX.matcher(it).matches())
            .map(UUID::fromString)
            .map(it -> Math.floorMod(it.getMostSignificantBits(), totalPartitions))
            .map(it -> it % totalPartitions)
            .orElse(0);
    }
}
