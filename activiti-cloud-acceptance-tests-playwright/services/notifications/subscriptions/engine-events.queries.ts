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

export const ENGINE_EVENTS_SUBSCRIPTION = `
subscription engineEvents(
  $serviceName: String!
  $eventTypes: [EngineEventType!]
  $businessKey: String!
  $processDefinitionKey: String!
  $actor: [String!]
) {
  engineEvents(
    serviceName: [$serviceName]
    eventType: $eventTypes
    businessKey: [$businessKey]
    processDefinitionKey: [$processDefinitionKey]
    actor: $actor
  ) {
    serviceName
    processDefinitionKey
    eventType
    actor
  }
}`;

export interface EngineEventsSubscriptionVariables {
    serviceName: string;
    eventTypes: string[];
    businessKey: string;
    processDefinitionKey: string;
    actor?: string[];
}
