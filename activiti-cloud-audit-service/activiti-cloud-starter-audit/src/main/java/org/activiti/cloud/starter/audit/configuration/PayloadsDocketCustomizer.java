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
package org.activiti.cloud.starter.audit.configuration;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.common.swagger.DocketCustomizer;
import org.activiti.cloud.services.audit.api.converters.CloudRuntimeEventModel;
import org.activiti.cloud.services.audit.api.converters.CloudRuntimeEventType;
import org.springframework.beans.factory.annotation.Autowired;
import springfox.documentation.schema.AlternateTypeRules;
import springfox.documentation.schema.WildcardType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Swagger Api Models
 */
public class PayloadsDocketCustomizer implements DocketCustomizer {

    @Autowired
    private TypeResolver typeResolver;

    public Docket customize(final Docket docket) {
        ResolvedType resourceTypeWithWildCard = typeResolver.resolve(
            CloudRuntimeEvent.class,
            WildcardType.class,
            CloudRuntimeEventType.class
        );

        return docket.alternateTypeRules(
            AlternateTypeRules.newRule(
                resourceTypeWithWildCard,
                CloudRuntimeEventModel.class
            )
        );
    }
}
