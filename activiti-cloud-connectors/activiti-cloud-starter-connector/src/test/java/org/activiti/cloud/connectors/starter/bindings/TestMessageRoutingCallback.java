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
package org.activiti.cloud.connectors.starter.bindings;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Optional;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

public class TestMessageRoutingCallback implements MessageRoutingCallback {

  public final static String MOCK_INTEGRATION_REQUEST_EVENT_BINDING = "mockTypeIntegrationRequestEvents";
  public final static String MOCK_INTEGRATION_RUNTIME_ERROR_BINDING = "mockTypeIntegrationRuntimeError";
  public final static String MOCK_INTEGRATION_ERROR_SENDER_BINDING = "mockTypeIntegrationErrorSender";
  public final static String MOCK_INTEGRATION_CLOUD_BPMN_ERROR_SENDER_BINDING = "mockTypeIntegrationCloudBpmnErrorSender";
  public final static String MOCK_INTEGRATION_CLOUD_BPMN_ERROR_ROOT_CAUSE_SENDER_BINDING = "mockTypeIntegrationCloudBpmnErrorRootCauseSender";
  public final static String MOCK_INTEGRATION_CLOUD_BPMN_ERROR_MESSAGE_SENDER_BINDING = "mockTypeIntegrationCloudBpmnErrorMessageSender";

  private final Map<String, String> typeBindings = Map.ofEntries(
      new SimpleEntry<>("Mock", MOCK_INTEGRATION_REQUEST_EVENT_BINDING),
      new SimpleEntry<>("RuntimeException", MOCK_INTEGRATION_RUNTIME_ERROR_BINDING),
      new SimpleEntry<>("Error", MOCK_INTEGRATION_ERROR_SENDER_BINDING),
      new SimpleEntry<>("CloudBpmnError", MOCK_INTEGRATION_CLOUD_BPMN_ERROR_SENDER_BINDING),
      new SimpleEntry<>("CloudBpmnErrorCause", MOCK_INTEGRATION_CLOUD_BPMN_ERROR_ROOT_CAUSE_SENDER_BINDING),
      new SimpleEntry<>("CloudBpmnErrorMessage", MOCK_INTEGRATION_CLOUD_BPMN_ERROR_MESSAGE_SENDER_BINDING)
  );

  private final String HEADER_TYPE = "type";

  @Override
  public FunctionRoutingResult routingResult(Message<?> message) {
    return new FunctionRoutingResult(getFunctionName(Optional.ofNullable(message.getHeaders())));
  }

  private String getFunctionName(Optional<MessageHeaders> headers){
    return headers.filter(messageHeaders -> messageHeaders.containsKey(HEADER_TYPE))
        .map(messageHeaders -> messageHeaders.get(HEADER_TYPE))
        .map(typeBindings::get)
        .orElseThrow(() -> new IllegalStateException(String.format("'%s' header was not recognized as functional binding", HEADER_TYPE)));
  }

}
