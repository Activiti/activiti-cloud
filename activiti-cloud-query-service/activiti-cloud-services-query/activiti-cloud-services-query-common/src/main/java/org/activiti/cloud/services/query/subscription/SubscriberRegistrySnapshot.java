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
package org.activiti.cloud.services.query.subscription;

import java.util.List;

/**
 * Read-only view of a query-rest instance's local subscriber registry. Exposed here, in query-common,
 * so the pushed-counts relay can replay presence as a {@code SubscriberRegistryMessage} SNAPSHOT
 * without a compile-time dependency on the query-rest starter: query-rest's {@code SubscriberRegistry}
 * implements it and the relay injects it by type.
 */
public interface SubscriberRegistrySnapshot {
    /** @return one {@link SubscriberRegistryMessage.Entry} per user currently live on this instance. */
    List<SubscriberRegistryMessage.Entry> snapshotEntries();
}
