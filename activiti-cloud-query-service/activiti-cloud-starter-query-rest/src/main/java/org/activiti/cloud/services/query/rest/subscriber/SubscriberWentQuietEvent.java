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
package org.activiti.cloud.services.query.rest.subscriber;

import java.time.Instant;

/**
 * Published when a user's last live websocket session on this instance closes (either by a
 * clean disconnect or by expiry - both paths converge on the same transition). The seam a
 * later step listens on to broadcast an {@code UNREGISTERED} message.
 */
public record SubscriberWentQuietEvent(String userId, Instant at) {}
