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
package org.activiti.cloud.services.audit.jpa.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.activiti.api.process.model.payloads.MessageEventPayload;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.runtime.model.impl.BPMNMessageImpl;
import org.activiti.api.runtime.model.impl.BPMNSignalImpl;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationRequestedEventImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VariablePayloadEventTest {

    @ParameterizedTest
    @ValueSource(strings = { "current", "previous", "both", "empty", "missing", "null-previous" })
    void should_detectAndClearVariablePayloads(String state) {
        var event = new VariableUpdatedEventEntity();
        event.setVariableName("payload");
        event.setVariableType("string");
        event.setProcessInstanceId("process");
        event.setTaskId("task");
        if (!state.equals("missing")) {
            String value = state.equals("current") || state.equals("both") ? "secret" : null;
            event.setVariableInstance(new VariableInstanceImpl<>("payload", "string", value, "process", "task"));
        }
        if (state.equals("previous") || state.equals("both")) {
            event.setPreviousValue(new VariableValue<>("previous secret"));
        } else if (state.equals("null-previous")) {
            event.setPreviousValue(new VariableValue<>(null));
        }
        boolean empty = state.equals("empty") || state.equals("missing");
        assertThat(event.isVariablePayloadEmpty()).isEqualTo(empty);

        event.clearVariablePayloads();
        event.clearVariablePayloads();

        assertThat(event.isVariablePayloadEmpty()).isTrue();
        assertThat(event.getPreviousValue()).isNull();
        assertThat(event.getVariableInstance().getName()).isEqualTo("payload");
        assertThat(event.getVariableInstance().getType()).isEqualTo("string");
        assertThat(event.getVariableInstance().getProcessInstanceId()).isEqualTo("process");
        assertThat(event.getVariableInstance().getTaskId()).isEqualTo("task");
        assertThat((Object) event.getVariableInstance().getValue()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "missing", "null", "empty", "inbound", "outbound", "both" })
    void should_detectAndClearIntegrationPayloads(String state) {
        IntegrationContextImpl context = state.equals("null")
            ? new IntegrationContextImpl() {
                  @Override
                  public Map<String, Object> getInBoundVariables() {
                      return null;
                  }

                  @Override
                  public Map<String, Object> getOutBoundVariables() {
                      return null;
                  }
              }
            : new IntegrationContextImpl();
        context.setId("context");
        if (state.equals("inbound") || state.equals("both")) {
            context.getInBoundVariables().put("input", "secret");
        }
        if (state.equals("outbound") || state.equals("both")) {
            context.getOutBoundVariables().put("output", "secret");
        }
        var cloudEvent = new CloudIntegrationRequestedEventImpl(context);
        cloudEvent.setSequenceNumber(0);
        var event = state.equals("missing")
            ? new IntegrationRequestSentEventEntity()
            : new IntegrationRequestSentEventEntity(cloudEvent);
        boolean empty = state.equals("missing") || state.equals("null") || state.equals("empty");
        assertThat(event.isVariablePayloadEmpty()).isEqualTo(empty);

        event.clearVariablePayloads();

        assertThat(event.isVariablePayloadEmpty()).isTrue();
        if (state.equals("missing")) {
            assertThat(event.getIntegrationContext()).isNull();
        } else {
            assertThat(event.getIntegrationContext()).isSameAs(context);
            assertThat(context.getId()).isEqualTo("context");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "missing", "no-payload", "null", "empty", "variables" })
    void should_clearMessageVariablesAndPreserveMetadata(String state) {
        var message = new BPMNMessageImpl("activity");
        Map<String, Object> variables = state.equals("null") ? null : Map.of();
        if (state.equals("variables")) {
            variables = Map.of("input", "secret");
        }
        var payload = new MessageEventPayload("notification", "correlation", "business", variables);
        String id = payload.getId();
        if (!state.equals("no-payload")) {
            message.setMessagePayload(payload);
        }
        var event = new MessageSentAuditEventEntity();
        if (!state.equals("missing")) {
            event.setMessage(message);
        }
        assertThat(event.isVariablePayloadEmpty()).isEqualTo(!state.equals("variables"));

        event.clearVariablePayloads();
        event.clearVariablePayloads();

        assertThat(event.isVariablePayloadEmpty()).isTrue();
        if (state.equals("missing")) {
            assertThat(event.getMessage()).isNull();
        } else {
            assertThat(event.getMessage()).isSameAs(message);
            assertThat(message.getElementId()).isEqualTo("activity");
            if (state.equals("no-payload")) {
                assertThat(message.getMessagePayload()).isNull();
            } else {
                assertThat(message.getMessagePayload()).isSameAs(payload);
                assertThat(payload.getId()).isEqualTo(id);
                assertThat(payload.getName()).isEqualTo("notification");
                assertThat(payload.getCorrelationKey()).isEqualTo("correlation");
                assertThat(payload.getBusinessKey()).isEqualTo("business");
                if (state.equals("null")) {
                    assertThat(payload.getVariables()).isNull();
                } else {
                    assertThat(payload.getVariables()).isEmpty();
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "missing", "no-payload", "null", "empty", "variables" })
    void should_clearSignalVariablesAndPreserveMetadata(String state) {
        var signal = new BPMNSignalImpl("activity");
        Map<String, Object> variables = state.equals("null") ? null : Map.of();
        if (state.equals("variables")) {
            variables = Map.of("input", "secret");
        }
        var payload = new SignalPayload("notification", variables);
        String id = payload.getId();
        if (!state.equals("no-payload")) {
            signal.setSignalPayload(payload);
        }
        var event = new SignalReceivedAuditEventEntity();
        if (!state.equals("missing")) {
            event.setSignal(signal);
        }
        assertThat(event.isVariablePayloadEmpty()).isEqualTo(!state.equals("variables"));

        event.clearVariablePayloads();
        event.clearVariablePayloads();

        assertThat(event.isVariablePayloadEmpty()).isTrue();
        if (state.equals("missing")) {
            assertThat(event.getSignal()).isNull();
        } else {
            assertThat(event.getSignal()).isSameAs(signal);
            assertThat(signal.getElementId()).isEqualTo("activity");
            if (state.equals("no-payload")) {
                assertThat(signal.getSignalPayload()).isNull();
            } else {
                assertThat(signal.getSignalPayload()).isSameAs(payload);
                assertThat(payload.getId()).isEqualTo(id);
                assertThat(payload.getName()).isEqualTo("notification");
                if (state.equals("null")) {
                    assertThat(payload.getVariables()).isNull();
                } else {
                    assertThat(payload.getVariables()).isEmpty();
                }
            }
        }
    }
}
