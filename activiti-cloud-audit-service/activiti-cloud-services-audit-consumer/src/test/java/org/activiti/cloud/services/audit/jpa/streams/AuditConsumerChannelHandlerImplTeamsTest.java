/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.audit.jpa.streams;

import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCreatedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
//@SpringBootTest(
//    classes = AuditJPAStreamsAutoConfiguration.class,
//    properties = {
//    "teams.connector.chat.enabled=true",
//    "microsoft.app.id=test-app-id",
//    "microsoft.app.password=test-password",
//    "teams.connector.tenant=test-tenant"
//})
public class AuditConsumerChannelHandlerImplTeamsTest {

    @InjectMocks
    private AuditConsumerChannelHandlerImpl handler;

    @Mock
    private EventsRepository eventsRepository;

    @Mock
    private APIEventToEntityConverters converters;

    @Captor
    private ArgumentCaptor<Iterable<AuditEventEntity>> argumentCaptor;

    //    @Test
    public void shouldSendTeamsMessageOnAuditEvent() {
        //given
        CloudRuntimeEvent cloudRuntimeEvent = Mockito.mock(CloudRuntimeEventImpl.class);
        Mockito.when(cloudRuntimeEvent.getEventType()).thenReturn(ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED);
        Mockito.when(cloudRuntimeEvent.getAppName()).thenReturn("TestApp");
        Mockito.when(cloudRuntimeEvent.getActor()).thenReturn(getJsonDetails());
        Mockito.when(cloudRuntimeEvent.getTimestamp()).thenReturn(new Date().getTime());
        Mockito.when(cloudRuntimeEvent.getProcessInstanceId()).thenReturn("ad4f8843-bb25-11f0-bebc-1ac6710786eb");
        Mockito.when(cloudRuntimeEvent.getId()).thenReturn("ad546a47-bb25-11f0-bebc-1ac6710786eb");

        EventToEntityConverter converter = Mockito.mock(EventToEntityConverter.class);
        Mockito
            .when(converters.getConverterByEventTypeName(ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED.name()))
            .thenReturn(converter);
        ProcessCreatedAuditEventEntity entity = Mockito.mock(ProcessCreatedAuditEventEntity.class);
        Mockito.when(converter.convertToEntity(cloudRuntimeEvent)).thenReturn(entity);

        CloudRuntimeEvent[] events = { cloudRuntimeEvent };

        //when
        handler.receiveCloudRuntimeEvent(
            new HashMap<String, Object>() {
                {
                    put("id", UUID.randomUUID());
                }
            },
            events
        );

        //then
        Mockito.verify(eventsRepository).saveAll(argumentCaptor.capture());
        Assertions.assertThat(argumentCaptor.getValue()).containsOnly(entity);
    }

    private String getJsonDetails() {
        return """
            {
              "entity": {
                "id": "ad546a47-bb25-11f0-bebc-1ac6710786eb",
                "inBoundVariables": {
                  "restUrl": {
                    "type": "string",
                    "value": "https://aaaaa.com"
                  }
                },
                "outBoundVariables": {},
                "processInstanceId": "ad4f8843-bb25-11f0-bebc-1ac6710786eb",
                "rootProcessInstanceId": "ad4f8843-bb25-11f0-bebc-1ac6710786eb",
                "processDefinitionId": "Process_1762442098730:1:a51db671-bb24-11f0-bebc-1ac6710786eb",
                "executionId": "ad502486-bb25-11f0-bebc-1ac6710786eb",
                "processDefinitionKey": "Process_1762442098730",
                "processDefinitionVersion": 1,
                "clientId": "restConnector_1o6p9s0",
                "clientType": "ServiceTask",
                "appVersion": "1",
                "connectorType": "rest-connector-e0r2c.POST"
              },
              "errorMessage": "No subject alternative DNS name matching aaaaa.com found.",
              "stackTraceElements": [
                {
                  "methodName": "matchDNS",
                  "fileName": "HostnameChecker.java",
                  "lineNumber": 207,
                  "nativeMethod": false,
                  "className": "sun.security.util.HostnameChecker"
                },
                {
                  "methodName": "match",
                  "fileName": "HostnameChecker.java",
                  "lineNumber": 103,
                  "nativeMethod": false,
                  "className": "sun.security.util.HostnameChecker"
                },
                {
                  "methodName": "checkIdentity",
                  "fileName": "X509TrustManagerImpl.java",
                  "lineNumber": 466,
                  "nativeMethod": false,
                  "className": "sun.security.ssl.X509TrustManagerImpl"
                },
                {
                  "methodName": "checkIdentity",
                  "fileName": "X509TrustManagerImpl.java",
                  "lineNumber": 417,
                  "nativeMethod": false,
                  "className": "sun.security.ssl.X509TrustManagerImpl"
                },
                {
                  "methodName": "checkTrusted",
                  "fileName": "X509TrustManagerImpl.java",
                  "lineNumber": 237,
                  "nativeMethod": false,
                  "className": "sun.security.ssl.X509TrustManagerImpl"
                },
                {
                  "methodName": "checkServerTrusted",
                  "fileName": "X509TrustManagerImpl.java",
                  "lineNumber": 132,
                  "nativeMethod": false,
                  "className": "sun.security.ssl.X509TrustManagerImpl"
                },
                {
                  "methodName": "checkServerCerts",
                  "fileName": "CertificateMessage.java",
                  "lineNumber": 631,
                  "nativeMethod": false,
                  "className": "sun.security.ssl.CertificateMessage$T12CertificateConsumer"
                },
                {
                  "methodName": "onCertificate",
                  "fileName": "CertificateMessage.java",
                  "lineNumber": 467,
                  "nativeMethod": false,
                  "className": "sun.security.ssl.CertificateMessage$T12CertificateConsumer"
                },
                {
                  "methodName": "consume",
                  "fileName": "CertificateMessage.java",
                  "lineNumber": 363,
                  "nativeMethod": false,
                  "className": "sun.security.ssl.CertificateMessage$T12CertificateConsumer"
                },
                {
                  "methodName": "consume",
                  "fileName": "SSLHandshake.java",
                  "lineNumber": 393,
                  "nativeMethod": false,
                  "className": "sun.security.ssl.SSLHandshake"
                },
                {
                  "methodName": "dispatch",
                  "fileName": "HandshakeContext.java",
                  "lineNumber": 477,
                  "nativeMethod": false,
                  "className": "sun.security.ssl.HandshakeContext"
                },
                {
                  "methodName": "dispatch",
                  "fileName": "HandshakeContext.java",
                  "lineNumber": 448,
                  "nativeMethod": false,
                  "className": "sun.security.ssl.HandshakeContext"
                },
                {
                  "methodName": "dispatch",
                  "fileName": "TransportContext.java",
                  "lineNumber": 206,
                  "nativeMethod": false,
                  "className": "sun.security.ssl.TransportContext"
                },
                {
                  "methodName": "decode",
                  "fileName": "SSLTransport.java",
                  "lineNumber": 172,
                  "nativeMethod": false,
                  "className": "sun.security.ssl.SSLTransport"
                },
                {
                  "methodName": "decode",
                  "fileName": "SSLSocketImpl.java",
                  "lineNumber": 1506,
                  "nativeMethod": false,
                  "className": "sun.security.ssl.SSLSocketImpl"
                },
                {
                  "methodName": "readHandshakeRecord",
                  "fileName": "SSLSocketImpl.java",
                  "lineNumber": 1421,
                  "nativeMethod": false,
                  "className": "sun.security.ssl.SSLSocketImpl"
                },
                {
                  "methodName": "startHandshake",
                  "fileName": "SSLSocketImpl.java",
                  "lineNumber": 455,
                  "nativeMethod": false,
                  "className": "sun.security.ssl.SSLSocketImpl"
                },
                {
                  "methodName": "startHandshake",
                  "fileName": "SSLSocketImpl.java",
                  "lineNumber": 426,
                  "nativeMethod": false,
                  "className": "sun.security.ssl.SSLSocketImpl"
                },
                {
                  "methodName": "executeHandshake",
                  "fileName": "AbstractClientTlsStrategy.java",
                  "lineNumber": 253,
                  "nativeMethod": false,
                  "className": "org.apache.hc.client5.http.ssl.AbstractClientTlsStrategy"
                },
                {
                  "methodName": "upgrade",
                  "fileName": "AbstractClientTlsStrategy.java",
                  "lineNumber": 211,
                  "nativeMethod": false,
                  "className": "org.apache.hc.client5.http.ssl.AbstractClientTlsStrategy"
                },
                {
                  "methodName": "upgrade",
                  "fileName": "DefaultClientTlsStrategy.java",
                  "lineNumber": 48,
                  "nativeMethod": false,
                  "className": "org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy"
                },
                {
                  "methodName": "connect",
                  "fileName": "DefaultHttpClientConnectionOperator.java",
                  "lineNumber": 219,
                  "nativeMethod": false,
                  "className": "org.apache.hc.client5.http.impl.io.DefaultHttpClientConnectionOperator"
                }
              ],
              "errorClassName": "org.activiti.cloud.api.process.model.CloudBpmnError"
            }
            """;
    }
}
