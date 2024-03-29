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

package org.activiti.cloud.services.query.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.services.query.model.VariableValue;
import org.junit.jupiter.api.Test;

public class VariableSearchTest {

    @Test
    public void isSet_shouldReturnTrue_when_variableNameAndValueAreSet() {
        //given
        VariableSearch variableSearch = new VariableSearch("var", new VariableValue<>("any"), "string");

        //when
        boolean isSet = variableSearch.isSet();

        //then
        assertThat(isSet).isTrue();
    }

    @Test
    public void isSet_shouldReturnFalse_when_variableNameIsNotSet() {
        //given
        VariableSearch variableSearch = new VariableSearch(null, new VariableValue<>("any"), "string");

        //when
        boolean isSet = variableSearch.isSet();

        //then
        assertThat(isSet).isFalse();
    }

    @Test
    public void isSet_shouldReturnFalse_when_variableValueIsNotSet() {
        //given
        VariableSearch variableSearch = new VariableSearch("var", null, "string");

        //when
        boolean isSet = variableSearch.isSet();

        //then
        assertThat(isSet).isFalse();
    }

    @Test
    public void isSet_shouldReturnFalse_when_variableValueIsWrappingANullValue() {
        //given
        VariableSearch variableSearch = new VariableSearch("var", new VariableValue<>(null), "string");

        //when
        boolean isSet = variableSearch.isSet();

        //then
        assertThat(isSet).isFalse();
    }
}
