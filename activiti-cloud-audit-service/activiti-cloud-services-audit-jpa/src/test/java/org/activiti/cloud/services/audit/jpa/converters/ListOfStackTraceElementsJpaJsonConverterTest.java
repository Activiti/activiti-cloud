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
package org.activiti.cloud.services.audit.jpa.converters;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.activiti.cloud.services.audit.jpa.converters.json.ListOfStackTraceElementsJpaJsonConverter;
import org.junit.jupiter.api.Test;

public class ListOfStackTraceElementsJpaJsonConverterTest {

    private ListOfStackTraceElementsJpaJsonConverter converter = new ListOfStackTraceElementsJpaJsonConverter();

    @Test
    public void convertToDatabaseColumnShouldReturnTheEntityJsonRepresentation() throws Exception {
        //given
        Error error = new Error("Message");

        error.fillInStackTrace();

        //when
        String jsonRepresentation = converter.convertToDatabaseColumn(Arrays.asList(error.getStackTrace()));

        //then
        assertThatJson(jsonRepresentation)
            .node("[0].methodName")
            .isEqualTo("convertToDatabaseColumnShouldReturnTheEntityJsonRepresentation");
    }

    @Test
    public void convertToEntityAttributeShouldCreateAProcessInstanceWithFieldsSet() throws Exception {
        //given
        String jsonRepresentation =
            "[{\"methodName\":\"convertToDatabaseColumnShouldReturnTheEntityJsonRepresentation\",\"fileName\":\"ListJpaJsonConverterTest.java\",\"lineNumber\":36,\"className\":\"org.activiti.cloud.services.audit.jpa.converters.ListJpaJsonConverterTest\",\"nativeMethod\":false},{\"methodName\":\"invoke0\",\"fileName\":\"NativeMethodAccessorImpl.java\",\"lineNumber\":-2,\"className\":\"sun.reflect.NativeMethodAccessorImpl\",\"nativeMethod\":true},{\"methodName\":\"invoke\",\"fileName\":\"NativeMethodAccessorImpl.java\",\"lineNumber\":62,\"className\":\"sun.reflect.NativeMethodAccessorImpl\",\"nativeMethod\":false},{\"methodName\":\"invoke\",\"fileName\":\"DelegatingMethodAccessorImpl.java\",\"lineNumber\":43,\"className\":\"sun.reflect.DelegatingMethodAccessorImpl\",\"nativeMethod\":false},{\"methodName\":\"invoke\",\"fileName\":\"Method.java\",\"lineNumber\":498,\"className\":\"java.lang.reflect.Method\",\"nativeMethod\":false},{\"methodName\":\"runReflectiveCall\",\"fileName\":\"FrameworkMethod.java\",\"lineNumber\":50,\"className\":\"org.junit.runners.model.FrameworkMethod$1\",\"nativeMethod\":false},{\"methodName\":\"run\",\"fileName\":\"ReflectiveCallable.java\",\"lineNumber\":12,\"className\":\"org.junit.internal.runners.model.ReflectiveCallable\",\"nativeMethod\":false},{\"methodName\":\"invokeExplosively\",\"fileName\":\"FrameworkMethod.java\",\"lineNumber\":47,\"className\":\"org.junit.runners.model.FrameworkMethod\",\"nativeMethod\":false},{\"methodName\":\"evaluate\",\"fileName\":\"InvokeMethod.java\",\"lineNumber\":17,\"className\":\"org.junit.internal.runners.statements.InvokeMethod\",\"nativeMethod\":false},{\"methodName\":\"runLeaf\",\"fileName\":\"ParentRunner.java\",\"lineNumber\":325,\"className\":\"org.junit.runners.ParentRunner\",\"nativeMethod\":false},{\"methodName\":\"runChild\",\"fileName\":\"BlockJUnit4ClassRunner.java\",\"lineNumber\":78,\"className\":\"org.junit.runners.BlockJUnit4ClassRunner\",\"nativeMethod\":false},{\"methodName\":\"runChild\",\"fileName\":\"BlockJUnit4ClassRunner.java\",\"lineNumber\":57,\"className\":\"org.junit.runners.BlockJUnit4ClassRunner\",\"nativeMethod\":false},{\"methodName\":\"run\",\"fileName\":\"ParentRunner.java\",\"lineNumber\":290,\"className\":\"org.junit.runners.ParentRunner$3\",\"nativeMethod\":false},{\"methodName\":\"schedule\",\"fileName\":\"ParentRunner.java\",\"lineNumber\":71,\"className\":\"org.junit.runners.ParentRunner$1\",\"nativeMethod\":false},{\"methodName\":\"runChildren\",\"fileName\":\"ParentRunner.java\",\"lineNumber\":288,\"className\":\"org.junit.runners.ParentRunner\",\"nativeMethod\":false},{\"methodName\":\"access$000\",\"fileName\":\"ParentRunner.java\",\"lineNumber\":58,\"className\":\"org.junit.runners.ParentRunner\",\"nativeMethod\":false},{\"methodName\":\"evaluate\",\"fileName\":\"ParentRunner.java\",\"lineNumber\":268,\"className\":\"org.junit.runners.ParentRunner$2\",\"nativeMethod\":false},{\"methodName\":\"run\",\"fileName\":\"ParentRunner.java\",\"lineNumber\":363,\"className\":\"org.junit.runners.ParentRunner\",\"nativeMethod\":false},{\"methodName\":\"run\",\"fileName\":\"JUnit4TestReference.java\",\"lineNumber\":89,\"className\":\"org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference\",\"nativeMethod\":false},{\"methodName\":\"run\",\"fileName\":\"TestExecution.java\",\"lineNumber\":41,\"className\":\"org.eclipse.jdt.internal.junit.runner.TestExecution\",\"nativeMethod\":false},{\"methodName\":\"runTests\",\"fileName\":\"RemoteTestRunner.java\",\"lineNumber\":541,\"className\":\"org.eclipse.jdt.internal.junit.runner.RemoteTestRunner\",\"nativeMethod\":false},{\"methodName\":\"runTests\",\"fileName\":\"RemoteTestRunner.java\",\"lineNumber\":763,\"className\":\"org.eclipse.jdt.internal.junit.runner.RemoteTestRunner\",\"nativeMethod\":false},{\"methodName\":\"run\",\"fileName\":\"RemoteTestRunner.java\",\"lineNumber\":463,\"className\":\"org.eclipse.jdt.internal.junit.runner.RemoteTestRunner\",\"nativeMethod\":false},{\"methodName\":\"main\",\"fileName\":\"RemoteTestRunner.java\",\"lineNumber\":209,\"className\":\"org.eclipse.jdt.internal.junit.runner.RemoteTestRunner\",\"nativeMethod\":false}]";

        //when
        List<StackTraceElement> stackTraceElements = converter.convertToEntityAttribute(jsonRepresentation);

        //then
        assertThat(stackTraceElements).isNotEmpty();
    }
}
