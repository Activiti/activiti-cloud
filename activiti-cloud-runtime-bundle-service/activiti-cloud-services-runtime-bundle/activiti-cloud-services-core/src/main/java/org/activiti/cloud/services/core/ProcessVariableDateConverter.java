/*
 * Copyright 2020 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.services.core;

import java.util.Date;

import org.activiti.common.util.DateFormatterProvider;

public class ProcessVariableDateConverter implements SpringProcessVariableValueConverter<Date> {

    private final DateFormatterProvider provider;

    public ProcessVariableDateConverter(DateFormatterProvider provider) {
        this.provider = provider;
    }

    @Override
    public Date convert(String source) {
        return provider.parse(source);
    }
}