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
package org.activiti.cloud.services.query.app.count;

import java.util.List;

/**
 * Where recomputed counts go. Separated from {@link TaskCountEmitter} so that deciding <em>what</em> to
 * recompute stays testable without a broker, and so a deployment can swap the transport.
 */
public interface TaskCountChangePublisher {
    /**
     * Publishes a batch of recomputed counts. Called after the event batch has committed, so throwing
     * here cannot roll anything back - implementations are expected to handle their own failures rather
     * than propagate.
     */
    void publish(List<TaskCountChangedEvent> changes);
}
