package org.activiti.cloud.services.audit.jpa.converters;

import org.activiti.api.process.model.Deployment;
import org.activiti.api.runtime.model.impl.DeploymentImpl;
import org.activiti.cloud.services.audit.jpa.converters.json.ApplicationJpaJsonConverter;
import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.activiti.test.Assertions.assertThat;

public class ApplicationJpaJsonConverterTest {

  private ApplicationJpaJsonConverter converter = new ApplicationJpaJsonConverter();

    @Test
    public void convertToDatabaseColumnShouldReturnTheEntityJsonRepresentation() throws Exception {
        //given
        DeploymentImpl deployment = new DeploymentImpl();
        deployment.setName("DeploymentName");
        deployment.setVersion(1);
        deployment.setId("DeploymentId");
        
        //when
        String jsonRepresentation = converter.convertToDatabaseColumn(deployment);

        //then
        assertThatJson(jsonRepresentation)
                .node("name").isEqualTo("DeploymentName")
                .node("version").isEqualTo(1)
                .node("id").isEqualTo("DeploymentId");
    }

    @Test
    public void convertToEntityAttributeShouldCreateAnApplicationWithFieldsSet() throws Exception {
        //given
        String jsonRepresentation =
                "{\"id\":\"DeploymentId\"," +
                        "\"version\":\"1\"," +
                        "\"name\":\"DeploymentName\"}";

        //when
        Deployment deployment = converter.convertToEntityAttribute(jsonRepresentation);

        //then
        assertThat(deployment)
                .isNotNull()
                .hasId("DeploymentId")
                .hasName("DeploymentName")
                .hasVersion(1);
    }
}