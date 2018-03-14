package org.activiti.cloud.services.query.events.handlers;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import org.activiti.cloud.services.query.events.AbstractProcessEngineEvent;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractProcessEngineEventTest {

    @Test
    public void shouldBeNoDuplicates(){
        Annotation[] annotations = AbstractProcessEngineEvent.class.getAnnotations();
        assertThat(annotations).isNotEmpty();
        annotations[3].getClass();
        JsonSubTypes jsonSubTypes = (JsonSubTypes)annotations[3];
        JsonSubTypes.Type[] types = jsonSubTypes.value();

        List<String> names = new ArrayList<>();
        List<Class<?>> classes = new ArrayList<>();

        for(JsonSubTypes.Type type: types){
            assertThat(names).as("name appears more than once in annotation").doesNotContain(type.name());
            names.add(type.name());
            assertThat(classes).as("class appears more than once in annotation").doesNotContain(type.value());
            classes.add(type.value());
        }
    }
}
