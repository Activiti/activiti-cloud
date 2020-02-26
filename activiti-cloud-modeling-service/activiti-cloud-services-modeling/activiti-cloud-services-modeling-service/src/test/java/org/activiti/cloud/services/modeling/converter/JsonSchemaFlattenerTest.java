package org.activiti.cloud.services.modeling.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.activiti.cloud.services.modeling.validation.JsonSchemaFlattener;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

public class JsonSchemaFlattenerTest {

    private JsonSchemaFlattener flattener = new JsonSchemaFlattener();

    @Test
    public void should_flattenProcessExtensionSchema() throws IOException {
        JSONObject schema =  getSchemaFromResource("/schema/process-extensions-schema.json");
        assertThat(schema).isNotNull();

        JSONObject flattenSchema = flattener.flatten(schema);
        assertThat(flattenSchema).isNotNull();

        JSONObject definitions = (JSONObject)flattenSchema.get("definitions");
        assertThat(definitions).isNotNull();
    }

    @Test
    public void should_flattenSchemaShouldAddDefiniitons_when_noDefinitionsPresent() throws IOException {

        JSONObject schema =  getTestJSONObjectWithoutDefinitions("classpath://schema/model-extensions-schema.json");
        assertThat(schema).isNotNull();

        JSONObject flattenSchema = flattener.flatten(schema);
        assertThat(flattenSchema).isNotNull();

        JSONObject definitions = (JSONObject)flattenSchema.get("definitions");
        assertThat(definitions).isNotNull();
    }

    @Test
    public void should_flattenSchemaShouldAddSection_when_definitionsArePresent() throws IOException {

        JSONObject schema =  getTestJSONObjectWithDefinitions("classpath://schema/model-extensions-schema.json");
        assertThat(schema).isNotNull();

        String sectionName =  flattener.getSectionNameFromFileName("schema/model-extensions-schema.json");

        JSONObject flattenSchema = flattener.flatten(schema);
        assertThat(flattenSchema).isNotNull();

        JSONObject definitions = (JSONObject)flattenSchema.get("definitions");
        assertThat(definitions).isNotNull();
        assertThat(definitions.has(sectionName)).isTrue();
    }

    private JSONObject getSchemaFromResource(String schemaFileName) throws IOException {

        return new JSONObject(new JSONTokener(new ClassPathResource(schemaFileName)
            .getInputStream()));
    }

    private JSONObject getTestJSONObjectWithoutDefinitions(String classPath) {
        JSONObject object = new JSONObject();

        JSONObject refObject = new JSONObject();
        refObject.put("$ref", classPath);

        JSONArray arrObject = new JSONArray();
        arrObject.put(refObject);

        object.put("name", "value");
        object.put("allOf", arrObject);

        return object;
    }

    private JSONObject getTestJSONObjectWithDefinitions(String classPath) {
        JSONObject object = new JSONObject();

        JSONObject refObject = new JSONObject();
        refObject.put("$ref", classPath);

        JSONArray arrObject = new JSONArray();
        arrObject.put(refObject);

        object.put("name", "value");
        object.put("allOf", arrObject);

        JSONObject definition = new JSONObject();
        definition.put("name1", "value1");
        definition.put("name2", "value2");
        definition.put("keywithref", refObject);

        JSONObject definitions = new JSONObject();
        definitions.put("definition1", definition);

        object.put("definitions", definitions);

        return object;
    }

}
