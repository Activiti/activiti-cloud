/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.audit.jpa.converters;

import java.io.Serializable;

import org.activiti.cloud.services.audit.jpa.converters.json.VariableJpaJsonConverter;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.runtime.api.model.impl.VariableInstanceImpl;
import org.junit.Test;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.activiti.test.Assertions.assertThat;

public class VariablesJpaJsonConverterTest {

    private VariableJpaJsonConverter converter = new VariableJpaJsonConverter();

    @Test
    public void convertToDatabaseColumnShouldReturnTheEntityJsonRepresentation() throws Exception {
        //given
        VariableInstanceImpl variable = new VariableInstanceImpl("var-name",
                                                                 "String",
                                                                 "my string value",
                                                                 "proc-inst-id");
        variable.setTaskId("task-id");

        //when
        String jsonRepresentation = converter.convertToDatabaseColumn(variable);

        //then
        assertThatJson(jsonRepresentation)
                .node("name").isEqualTo("var-name")
                .node("type").isEqualTo("String")
                .node("value").isEqualTo("my string value")
                .node("taskId").isEqualTo("task-id")
                .node("processInstanceId").isEqualTo("proc-inst-id");
    }

    @Test
    public void convertToEntityAttributeShouldCreateAProcessInstanceWithFieldsSet() throws Exception {
        //given
        String jsonRepresentation =
                "{\"name\":\"var-name\"," +
                        "\"type\":\"String\"," +
                        "\"value\":\"my string value\"," +
                        "\"taskId\":\"task-id\"," +
                        "\"processInstanceId\":\"proc-inst-id\"}";

        //when
        VariableInstance variableInstance = converter.convertToEntityAttribute(jsonRepresentation);

        //then
        assertThat(variableInstance)
                .isNotNull()
                .hasType("String")
                .hasName("var-name")
                .hasValue("my string value")
                .hasProcessInstanceId("proc-inst-id")
                .hasTaskId("task-id");
    }

    @Test
    public void converterShouldDealWithDifferentTypes() throws Exception {

        Invoice invoice = new Invoice("inv-id", "customer");
        //given
        VariableInstanceImpl variable = new VariableInstanceImpl("var-name",
                                                                 "Invoice",
                                                                 invoice,
                                                                 "proc-inst-id");
        variable.setTaskId("task-id");

        //when
        String jsonRepresentation = converter.convertToDatabaseColumn(variable);

        //then
        assertThatJson(jsonRepresentation)
                .node("name").isEqualTo("var-name")
                .node("type").isEqualTo("Invoice")
                .node("value").isEqualTo(invoice)
                .node("taskId").isEqualTo("task-id")
                .node("processInstanceId").isEqualTo("proc-inst-id");
    }



    private class Invoice implements Serializable{
        private String id;
        private String customer;

        public Invoice() {
        }

        public Invoice(String id,
                       String customer) {
            this.id = id;
            this.customer = customer;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCustomer() {
            return customer;
        }

        public void setCustomer(String customer) {
            this.customer = customer;
        }
    }
}