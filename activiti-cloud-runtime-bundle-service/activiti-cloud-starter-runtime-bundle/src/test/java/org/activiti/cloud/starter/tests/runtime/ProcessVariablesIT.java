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
package org.activiti.cloud.starter.tests.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.builders.MessagePayloadBuilder;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.model.impl.ActivitiErrorMessageImpl;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.api.model.ProcessVariableValue;
import org.activiti.cloud.services.common.security.keycloak.test.support.WithMockKeycloakUser;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starter.tests.helper.MessageRestTemplate;
import org.activiti.cloud.starter.tests.helper.ProcessDefinitionRestTemplate;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.util.VariablesUtil;
import org.activiti.common.util.DateFormatterProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource({"classpath:application-test.properties", "classpath:access-control.properties"})
@DirtiesContext
@ContextConfiguration(classes = RuntimeITConfiguration.class,initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class ProcessVariablesIT {
    private static final String LONG = "long";
    private static final String INTEGER = "integer";
    private static final String JSON = "json";
    private static final String STRING = "string";
    private static final String DOUBLE = "double";
    private static final String BOOLEAN = "boolean";
    private static final String INT = "int";
    private static final String BIG_DECIMAL_VARIABLE = "bigDecimalVariable";
    private static final String DATE_VARIABLE = "dateVariable";
    private static final String BOOLEAN_VARIABLE = "booleanVariable";
    private static final String DOUBLE_VARIABLE = "doubleVariable";
    private static final String LONG_VARIABLE = "longVariable";
    private static final String INT_VARIABLE = "intVariable";
    private static final String STRING_VARIABLE = "stringVariable";
    private static final String JSON_VARIABLE = "jsonVariable";
    private static final String START_MESSAGE = "startMessage";
    private static final String DATE_1970_01_01T01_01_01_001Z = "1970-01-01T01:01:01.001Z";

    @Autowired
    private KeycloakTokenProducer keycloakSecurityContextClientRequestInterceptor;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private ProcessDefinitionRestTemplate processDefinitionRestTemplate;

    @Autowired
    private MessageRestTemplate messageRestTemplate;

    @Autowired
    private VariablesUtil variablesUtil;

    @Autowired
    private ProcessRuntime processRuntime;

    @Autowired
    private DateFormatterProvider dateFormatterProvider;

    private Map<String, String> processDefinitionIds = new HashMap<>();

    private static final String SIMPLE_PROCESS= "SimpleProcess";
    private static final String PROCESS_WITH_VARIABLES2 = "ProcessWithVariables2";
    private static final String PROCESS_WITH_EXTENSION_VARIABLES = "ProcessWithExtensionVariables";

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hruser");

        ResponseEntity<PagedModel<CloudProcessDefinition>> processDefinitions = processDefinitionRestTemplate.getProcessDefinitions();
        assertThat(processDefinitions.getStatusCode()).isEqualTo(HttpStatus.OK);
        for (ProcessDefinition pd : processDefinitions.getBody().getContent()) {
            processDefinitionIds.put(pd.getKey(),
                                     pd.getId());
        }
    }

    @Test
    public void shouldRetrieveProcessVariablesWithPermission() throws IOException {

        //given
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName",
                "Pedro");
        variables.put("lastName",
                "Silva");
        variables.put("age",
                15);
        variables.put("boolvar",
                true);
        variables.put("customPojo", objectMapper.readTree("{ \"test-json-variable-element1\":\"test-json-variable-value1\"}")
        );
        ResponseEntity<CloudProcessInstance> startResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(PROCESS_WITH_VARIABLES2),
                variables);

        await().untilAsserted(() -> {

            //when
            ResponseEntity<CollectionModel<CloudVariableInstance>> variablesEntity = processInstanceRestTemplate.getVariables(startResponse);
            Collection<CloudVariableInstance> variableCollection = variablesEntity.getBody().getContent();

            assertThat(variableCollection).isNotEmpty();
            assertThat(variablesContainEntry("firstName",
                    "Pedro",
                    variableCollection)).isTrue();
            assertThat(variablesContainEntry("lastName",
                    "Silva",
                    variableCollection)).isTrue();
            assertThat(variablesContainEntry("age",
                    15,
                    variableCollection)).isTrue();
            assertThat(variablesContainEntry("boolVar",
                    true,
                    variableCollection)).isTrue();

            assertThat(variableCollection)
                    .filteredOn("name", "customPojo")
                    .hasSize(1)
                    .extracting("value")
                    .hasOnlyElementsOfType(LinkedHashMap.class)
                    .first()
                    .toString()
                    .equalsIgnoreCase("{ \"test-json-variable-element1\":\"test-json-variable-value1\"}");

        });
    }

    private boolean variablesContainEntry(String key,
                                          Object value,
                                          Collection<CloudVariableInstance> variableCollection) {
        Iterator<CloudVariableInstance> iterator = variableCollection.iterator();
        while (iterator.hasNext()) {
            VariableInstance variable = iterator.next();
            if (variable.getName().equalsIgnoreCase(key) && variable.getValue().equals(value)) {
                assertThat(variable.getType()).isEqualToIgnoringCase(variable.getValue().getClass().getSimpleName());
                return true;
            }
        }
        return false;
    }

    @Test
    public void adminShouldDeleteProcessVariables() {
        //given
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName",
                "Peter");
        variables.put("lastName",
                "Silver");
        variables.put("age",
                19);

        List<String> variablesNames = new ArrayList<>(variables.keySet());

        ResponseEntity<CloudProcessInstance> startResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(PROCESS_WITH_VARIABLES2),
                variables);

        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");

        ResponseEntity<Void> variablesResponse = processInstanceRestTemplate.adminRemoveVariables(startResponse.getBody().getId(), variablesNames);

        //then
        assertThat(variablesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void shouldUpdateProcessVariables() {
        //given
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName",
                "Peter");
        variables.put("lastName",
                "Silver");
        variables.put("age",
                19);
        ResponseEntity<CloudProcessInstance> startResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(PROCESS_WITH_VARIABLES2),
                variables);

        variables.put("firstName",
                "Kermit");
        variables.put("lastName",
                "Frog");
        variables.put("age",
                100);

        //when
        processInstanceRestTemplate.setVariables(startResponse.getBody().getId(),
                variables);

        await().untilAsserted(() -> {

            // when
            ResponseEntity<CollectionModel<CloudVariableInstance>> variablesResponse = processInstanceRestTemplate.getVariables(startResponse);

            // then
            Collection<CloudVariableInstance> variableCollection = variablesResponse.getBody().getContent();

            assertThat(variableCollection).isNotEmpty();
            assertThat(variablesContainEntry("firstName",
                    "Kermit",
                    variableCollection)).isTrue();
            assertThat(variablesContainEntry("lastName",
                    "Frog",
                    variableCollection)).isTrue();
            assertThat(variablesContainEntry("age",
                    100,
                    variableCollection)).isTrue();
        });
    }

    @Test
    public void adminShouldUpdateProcessVariables() {
        //given
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hradmin");

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName",
                "Peter");
        variables.put("lastName",
                "Silver");
        variables.put("age",
                19);
        ResponseEntity<CloudProcessInstance> startResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(PROCESS_WITH_VARIABLES2),
                variables);

        variables.put("firstName",
                "Kermit");
        variables.put("lastName",
                "Frog");
        variables.put("age",
                100);

        //when
        processInstanceRestTemplate.adminSetVariables(startResponse.getBody().getId(),
                variables);

        await().untilAsserted(() -> {

            // when
            ResponseEntity<CollectionModel<CloudVariableInstance>> variablesResponse = processInstanceRestTemplate.getVariables(startResponse);

            // then
            Collection<CloudVariableInstance> variableCollection = variablesResponse.getBody().getContent();

            assertThat(variableCollection).isNotEmpty();
            assertThat(variablesContainEntry("firstName",
                    "Kermit",
                    variableCollection)).isTrue();
            assertThat(variablesContainEntry("lastName",
                    "Frog",
                    variableCollection)).isTrue();
            assertThat(variablesContainEntry("age",
                    100,
                    variableCollection)).isTrue();
        });
    }


    @Test
    public void shouldNotRetrieveProcessVariablesWithoutPermission() {
        //given
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName",
                "Fozzy");
        variables.put("lastName",
                "Bear");
        variables.put("age",
                22);
        ResponseEntity<CloudProcessInstance> startResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(PROCESS_WITH_VARIABLES2),
                variables);

        //testuser doesn't have permission according to access-control.properties
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testuser");

        await().untilAsserted(() -> {

            ResponseEntity<ActivitiErrorMessageImpl> variablesResponse = processInstanceRestTemplate.callGetVariablesWithErrorResponse(startResponse.getBody().getId());
            assertThat(variablesResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        });
    }

    @Test
    public void adminShouldSeeVariables() {
        //given
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName",
                "Rowlf");
        variables.put("lastName",
                "Dog");
        variables.put("age",
                5);
        ResponseEntity<CloudProcessInstance> startResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(PROCESS_WITH_VARIABLES2),
                variables);

        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");

        //should see at /{processInstanceId}/variables
        await().untilAsserted(() -> {

            // when
            ResponseEntity<CollectionModel<CloudVariableInstance>> variablesResponse = processInstanceRestTemplate.getVariables(startResponse);

            // then
            Collection<CloudVariableInstance> variableCollection = variablesResponse.getBody().getContent();

            assertThat(variableCollection).isNotEmpty();
            assertThat(variablesContainEntry("firstName",
                    "Rowlf",
                    variableCollection)).isTrue();
            assertThat(variablesContainEntry("lastName",
                    "Dog",
                    variableCollection)).isTrue();
            assertThat(variablesContainEntry("age",
                    5,
                    variableCollection)).isTrue();
        });
    }

    @Test
    public void shouldProperHandleProcessVariablesForAdmin() throws Exception {
        //given
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hradmin");
        checkProcessVariables(true);
    }

    @Test
    public void shouldProperHandleProcessVariables() throws Exception {
        //given
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hruser");
        checkProcessVariables(false);
    }

    private void setVariables(String processInstanceId,
                              boolean isAdmin,
                              Map<String, Object> variables) {
        if (isAdmin) {
            processInstanceRestTemplate.adminSetVariables(processInstanceId,
                    variables);
        } else {
            processInstanceRestTemplate.setVariables(processInstanceId,
                    variables);
        }
    }

    private void updateSimpleVariables(boolean isAdmin,
                                       String processInstanceId) {
        Map<String, Object> variables = new HashMap<>();

        variables.put("variableInt",
                2);
        variables.put("variableStr",
                "new value");
        variables.put("variableBool",
                false);

        setVariables(processInstanceId,
                isAdmin,
                variables);

        await().untilAsserted(() -> {
            //when
            ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = processInstanceRestTemplate.getVariables(processInstanceId);
            //then
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getContent())
                    .isNotNull()
                    .extracting(CloudVariableInstance::getName,
                            CloudVariableInstance::getValue)
                    .contains(tuple("variableInt",
                            2),
                            tuple("variableStr",
                                    "new value"),
                            tuple("variableBool",
                                    false));
        });
    }

    private void updateDateVariableWithADate(boolean isAdmin,
                                             String processInstanceId) {
        Map<String, Object> variables = new HashMap<>();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = new Date();

        variables.put("variableDate",
                date);

        setVariables(processInstanceId,
                isAdmin,
                variables);

        await().untilAsserted(() -> {
            //when
            ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = processInstanceRestTemplate.getVariables(processInstanceId);
            assertThat(responseEntity.getBody()).isNotNull();

            CloudVariableInstance var = responseEntity.getBody().getContent()
                    .stream()
                    .filter(v -> v.getName().equals("variableDate"))
                    .findAny()
                    .get();

            assertThat(var.getType()).isEqualTo("date");

            String dStr = format.format(date);
            assertThat(dStr).isEqualTo(var.getValue());
        });
    }

    private void updateDateVariableWithAFormattedString(boolean isAdmin,
                                                        String processInstanceId) throws Exception {

        Map<String, Object> variables = new HashMap<>();

        Date date = new Date();

        variables.put("variableDate",
                variablesUtil.getDateTimeFormattedString(date));

        setVariables(processInstanceId,
                isAdmin,
                variables);

        await().untilAsserted(() -> {
            //when
            ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = processInstanceRestTemplate.getVariables(processInstanceId);
            assertThat(responseEntity.getBody()).isNotNull();

            CloudVariableInstance variable = responseEntity.getBody().getContent()
                    .stream()
                    .filter(var -> var.getName().equals("variableDate"))
                    .findAny()
                    .get();

            assertThat(variable.getType()).isEqualTo("date");
            assertThat(variablesUtil.getExpectedDateTimeFormattedString(date)).isEqualTo(variable.getValue());
        });
    }

    private void checkProcessVariables(boolean isAdmin) throws Exception {

        ResponseEntity<CloudProcessInstance> processInstanceResponseEntity = processInstanceRestTemplate.startProcess(
                ProcessPayloadBuilder.start()
                        .withProcessDefinitionKey(PROCESS_WITH_EXTENSION_VARIABLES)
                        .withBusinessKey("businessKey")
                        .build());

        await().untilAsserted(() -> {
            //when
            ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = processInstanceRestTemplate.getVariables(processInstanceResponseEntity);
            //then
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getContent())
                    .isNotNull()
                    .extracting(CloudVariableInstance::getName,
                            CloudVariableInstance::getType)
                    .containsOnly(tuple("variableInt",
                            INTEGER),
                            tuple("variableStr",
                                    STRING),
                            tuple("variableBool",
                                    BOOLEAN),
                            tuple("variableDateTime",
                                    "date"),
                            tuple("variableDate",
                                    "date"));
        });

        //when update simple existing variables
        updateSimpleVariables(isAdmin, processInstanceResponseEntity.getBody().getId());

        updateDateVariableWithADate(isAdmin, processInstanceResponseEntity.getBody().getId());

        updateDateVariableWithAFormattedString(isAdmin, processInstanceResponseEntity.getBody().getId());

        //cleanup
        processInstanceRestTemplate.delete(processInstanceResponseEntity);
    }

    @Test
    public void shouldStartProcessWihDateVariables() throws Exception {
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hruser");
        checkStartProcessWihDateVariables(false);
    }

    @Test
    public void shouldStartProcessWihDateVariablesFromAdmin() throws Exception {
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hradmin");
        checkStartProcessWihDateVariables(true);
    }

    private void checkStartProcessWihDateVariables(boolean isAdmin) throws Exception {
        Map<String, Object> variables = new HashMap<>();
        Date date = new Date();

        variables.put("variableInt",
                2);
        variables.put("variableStr",
                "new value");
        variables.put("variableBool",
                false);
        variables.put("variableDateTime",
                variablesUtil.getDateTimeFormattedString(date));
        variables.put("variableDate",
                variablesUtil.getDateFormattedString(date));

        ResponseEntity<CloudProcessInstance> processInstanceResponseEntity;
        if (isAdmin) {
            processInstanceResponseEntity = processInstanceRestTemplate.adminStartProcess(ProcessPayloadBuilder.start()
                    .withProcessDefinitionKey(PROCESS_WITH_EXTENSION_VARIABLES)
                    .withProcessDefinitionId(processDefinitionIds.get(PROCESS_WITH_EXTENSION_VARIABLES))
                    .withBusinessKey("businessKey")
                    .withVariables(variables)
                    .build());
        } else {
            processInstanceResponseEntity = processInstanceRestTemplate.startProcess(ProcessPayloadBuilder.start()
                    .withProcessDefinitionKey(PROCESS_WITH_EXTENSION_VARIABLES)
                    .withProcessDefinitionId(processDefinitionIds.get(PROCESS_WITH_EXTENSION_VARIABLES))
                    .withBusinessKey("businessKey")
                    .withVariables(variables)
                    .build());
        }

        await().untilAsserted(() -> {
            //when
            ResponseEntity<CollectionModel<CloudVariableInstance>> responseEntity = processInstanceRestTemplate.getVariables(processInstanceResponseEntity);
            //then
            assertThat(responseEntity.getBody()).isNotNull();
            assertThat(responseEntity.getBody().getContent())
                    .isNotNull()
                    .extracting(CloudVariableInstance::getName,
                            CloudVariableInstance::getType,
                            CloudVariableInstance::getValue)
                    .contains(tuple("variableInt",
                            INTEGER,
                            2),
                            tuple("variableStr",
                                    STRING,
                                    "new value"),
                            tuple("variableBool",
                                    BOOLEAN,
                                    false),
                            tuple("variableDateTime",
                                    "date",
                                    variablesUtil.getExpectedDateTimeFormattedString(date)),
                            tuple("variableDate",
                                    "date",
                                    variablesUtil.getExpectedDateFormattedString(date)));
        });

        processInstanceRestTemplate.delete(processInstanceResponseEntity);
    }

    @Test
    public void shouldGetBADREQUESTOnStartProcessWihWrongDateVariables() throws Exception {
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hruser");
        checkBADREQUESTStartProcessWihWrongDateVariables(false);
    }

    @Test
    public void shouldGetBADREQUESTOnStartProcessWihWrongDateVariablesForAdmin() throws Exception {
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hradmin");
        checkBADREQUESTStartProcessWihWrongDateVariables(true);
    }

    private void checkBADREQUESTStartProcessWihWrongDateVariables(boolean isAdmin) throws Exception {
        Map<String, Object> variables = new HashMap<>();

        variables.put("variableBool",
                false);
        variables.put("variableDate",
                "WrongDateString");
        variables.put("variableDateTime",
                "WrongDateString");

        ResponseEntity<ActivitiErrorMessageImpl> responseEntity;
        if (isAdmin) {
            responseEntity = processInstanceRestTemplate.adminStartProcessWithErrorResponse(ProcessPayloadBuilder.start()
                    .withProcessDefinitionKey(PROCESS_WITH_EXTENSION_VARIABLES)
                    .withProcessDefinitionId(processDefinitionIds.get(PROCESS_WITH_EXTENSION_VARIABLES))
                    .withBusinessKey("businessKey")
                    .withVariables(variables)
                    .build());
        } else {
            responseEntity = processInstanceRestTemplate.startProcessWithErrorResponse(ProcessPayloadBuilder.start()
                    .withProcessDefinitionKey(PROCESS_WITH_EXTENSION_VARIABLES)
                    .withProcessDefinitionId(processDefinitionIds.get(PROCESS_WITH_EXTENSION_VARIABLES))
                    .withBusinessKey("businessKey")
                    .withVariables(variables)
                    .build());
        }

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody().getMessage()).contains("variableDate");
        assertThat(responseEntity.getBody().getMessage()).contains("variableDateTime");
    }

    @Test
    @WithMockKeycloakUser(username = "hruser", roles = "ACTIVITI_USER")
    public void testStartProcessVariablesPayloadConverter() {
        // given
        StartProcessPayload startProcessPayload = testStartProcessPayload();


        // when
        ResponseEntity<CloudProcessInstance> processInstanceResponseEntity = processInstanceRestTemplate.startProcess(startProcessPayload);
        CloudProcessInstance processInstance = processInstanceResponseEntity.getBody();

        // then
        List<VariableInstance> variableInstances = processRuntime.variables(ProcessPayloadBuilder.variables()
                                                                                                 .withProcessInstance(processInstance)
                                                                                                 .build());
        assertStartProcessPayloadVariablesAreConverted(variableInstances);

        // cleanup
        processRuntime.delete(ProcessPayloadBuilder.delete(processInstance.getId()));
    }

    @Test
    @WithMockKeycloakUser(username = "hruser", roles = "ACTIVITI_USER")
    public void testAdminStartProcessVariablesPayloadConverter() {
        // given
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hradmin");

        StartProcessPayload startProcessPayload = testStartProcessPayload();

        // when
        CloudProcessInstance processInstance = processInstanceRestTemplate.adminStartProcess(startProcessPayload)
                                                                          .getBody();
        // then
        List<VariableInstance> variableInstances = processRuntime.variables(ProcessPayloadBuilder.variables()
                                                                                                 .withProcessInstance(processInstance)
                                                                                                 .build());
        assertStartProcessPayloadVariablesAreConverted(variableInstances);

        // cleanup
        processRuntime.delete(ProcessPayloadBuilder.delete(processInstance.getId()));
    }

    @Test
    @WithMockKeycloakUser(username = "hruser", roles = "ACTIVITI_USER")
    public void testStartMessageVariablesPayloadConverter() {
        // given
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hruser");

        StartMessagePayload startMessagePayload = testStartMessagePayload();

        // when
        CloudProcessInstance processInstance = messageRestTemplate.message(startMessagePayload)
                                                                  .getBody();
        // then
        List<VariableInstance> variableInstances = processRuntime.variables(ProcessPayloadBuilder.variables()
                                                                                                 .withProcessInstance(processInstance)
                                                                                                 .build());
        assertStartProcessPayloadVariablesAreConverted(variableInstances);

        // cleanup
        processRuntime.delete(ProcessPayloadBuilder.delete(processInstance.getId()));
    }

    @Test
    @WithMockKeycloakUser(username = "hruser", roles = "ACTIVITI_USER")
    public void testAdminStartMessageVariablesPayloadConverter() {
        // given
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hradmin");

        StartMessagePayload startMessagePayload = testStartMessagePayload();

        // when
        CloudProcessInstance processInstance = messageRestTemplate.adminMessage(startMessagePayload)
                                                                  .getBody();
        // then
        List<VariableInstance> variableInstances = processRuntime.variables(ProcessPayloadBuilder.variables()
                                                                                                 .withProcessInstance(processInstance)
                                                                                                 .build());
        assertStartProcessPayloadVariablesAreConverted(variableInstances);

        // cleanup
        processRuntime.delete(ProcessPayloadBuilder.delete(processInstance.getId()));
    }

    private StartMessagePayload testStartMessagePayload() {
        return MessagePayloadBuilder.start(START_MESSAGE)
                                    .withVariables(testProcessVariableValues())
                                    .withVariable("correlationKey", "12345")
                                    .build();
    }

    private StartProcessPayload testStartProcessPayload() {
        return ProcessPayloadBuilder.start()
                                    .withProcessDefinitionKey(SIMPLE_PROCESS)
                                    .withVariables(testProcessVariableValues())
                                    .build();
    }

    private Map<String, Object> testProcessVariableValues() {
        Map<String, Object> variables = new LinkedHashMap<>();

        ProcessVariableValue jsonValue = new ProcessVariableValue(JSON, "{}");
        Map<String, String> stringValue = new ProcessVariableValue(STRING, "name").toMap();
        Map<String, String> intValue = new ProcessVariableValue(INTEGER, "10").toMap();
        Map<String, String> longValue = new ProcessVariableValue(LONG, "10").toMap();
        Map<String, String> booleanValue = new ProcessVariableValue(BOOLEAN, "true").toMap();
        Map<String, String> doubleValue = new ProcessVariableValue(DOUBLE, "10.00").toMap();
        Map<String, String> dateValue = new ProcessVariableValue("date", DATE_1970_01_01T01_01_01_001Z).toMap();
        Map<String, String> bigDecimalValue = new ProcessVariableValue("bigdecimal", "10.00").toMap();

        variables.put(JSON_VARIABLE, jsonValue);
        variables.put(STRING_VARIABLE, stringValue);
        variables.put(INT_VARIABLE, intValue);
        variables.put(LONG_VARIABLE, longValue);
        variables.put(DOUBLE_VARIABLE, doubleValue);
        variables.put(BOOLEAN_VARIABLE, booleanValue);
        variables.put(DATE_VARIABLE, dateValue);
        variables.put(BIG_DECIMAL_VARIABLE, bigDecimalValue);
        variables.put(INT, 10);
        variables.put(INTEGER, 10);
        variables.put(BOOLEAN, true);
        variables.put(DOUBLE, 10.00);
        variables.put(STRING, "name");
        variables.put(JSON, Collections.singletonMap("key", "data"));

        return variables;
    }

    private void assertStartProcessPayloadVariablesAreConverted(List<VariableInstance> variableInstances) {
        assertThat(variableInstances).isNotNull()
                                     .extracting("name", "value")
                                     .contains(tuple(JSON_VARIABLE, JsonNodeFactory.instance.objectNode()),
                                               tuple(STRING_VARIABLE, "name"),
                                               tuple(INT_VARIABLE, 10),
                                               tuple(LONG_VARIABLE, 10L),
                                               tuple(DOUBLE_VARIABLE, 10.00),
                                               tuple(BOOLEAN_VARIABLE, true),
                                               tuple(DATE_VARIABLE, dateFormatterProvider.parse(DATE_1970_01_01T01_01_01_001Z)),
                                               tuple(BIG_DECIMAL_VARIABLE, BigDecimal.valueOf(1000, 2)),
                                               tuple(INT, 10),
                                               tuple(INTEGER, 10),
                                               tuple(DOUBLE, 10.00),
                                               tuple(BOOLEAN, true),
                                               tuple(STRING, "name"),
                                               tuple(JSON, Collections.singletonMap("key", "data"))
                                     );

    }
}
