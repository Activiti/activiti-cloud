package org.activiti.cloud.services.modeling.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.cloud.services.modeling.validation.JsonSchemaFlattener;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

public class JsonSchemaFlattenerTest {

    // TODO pass to the constructor the bean objectMapper
    private ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    private JsonSchemaFlattener flattener = new JsonSchemaFlattener();

    @Test
    public void should_flattenProcessExtensionSchema() throws IOException {
        ObjectNode schema =  getSchemaFromResource("/schema/process-extensions-schema.json");
        assertThat(schema).isNotNull();

        ObjectNode flattenSchema = flattener.flatten(schema);
        assertThat(flattenSchema).isNotNull();

        ObjectNode definitions = (ObjectNode)flattenSchema.get("definitions");
        assertThat(definitions).isNotNull();
    }

    @Test
    public void should_flattenSchemaShouldAddDefiniitons_when_noDefinitionsPresent() throws IOException {

        ObjectNode schema =  getTestJSONObjectWithoutDefinitions("classpath://schema/model-extensions-schema.json");
        assertThat(schema).isNotNull();

        ObjectNode flattenSchema = flattener.flatten(schema);
        assertThat(flattenSchema).isNotNull();

        ObjectNode definitions = (ObjectNode)flattenSchema.get("definitions");
        assertThat(definitions).isNotNull();
    }

    @Test
    public void should_flattenSchemaShouldAddSection_when_definitionsArePresent() throws IOException {

        ObjectNode schema =  getTestJSONObjectWithDefinitions("classpath://schema/model-extensions-schema.json");
        assertThat(schema).isNotNull();

        String sectionName =  flattener.getSectionNameFromFileName("schema/model-extensions-schema.json");

        ObjectNode flattenSchema = flattener.flatten(schema);
        assertThat(flattenSchema).isNotNull();

        ObjectNode definitions = (ObjectNode)flattenSchema.get("definitions");
        assertThat(definitions).isNotNull();
        assertThat(definitions.has(sectionName)).isTrue();
    }

    private ObjectNode getSchemaFromResource(String schemaFileName) throws IOException {

        try (InputStream schemaInputStream = new ClassPathResource(schemaFileName).getInputStream()) {
            return  mapper.readValue(schemaInputStream, ObjectNode.class);
        }
    }

    private ObjectNode getTestJSONObjectWithoutDefinitions(String classPath) {
        ObjectNode object = mapper.createObjectNode();

        ObjectNode refObject = mapper.createObjectNode();
        refObject.put("$ref", classPath);

        ArrayNode arrObject = mapper.createArrayNode();
        arrObject.add(refObject);

        object.put("name", "value");
        object.put("allOf", arrObject);

        return object;
    }

    private ObjectNode getTestJSONObjectWithDefinitions(String classPath) {
        ObjectNode object = mapper.createObjectNode();

        ObjectNode refObject = mapper.createObjectNode();
        refObject.put("$ref", classPath);

        ArrayNode arrObject = mapper.createArrayNode();
        arrObject.add(refObject);

        object.put("name", "value");
        object.put("allOf", arrObject);

        ObjectNode definition = mapper.createObjectNode();
        definition.put("name1", "value1");
        definition.put("name2", "value2");
        definition.put("keywithref", refObject);

        ObjectNode definitions = mapper.createObjectNode();
        definitions.put("definition1", definition);

        object.put("definitions", definitions);

        return object;
    }

}
