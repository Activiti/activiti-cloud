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

package org.activiti.cloud.starter.query.configuration;

import static springfox.documentation.schema.AlternateTypeRules.newRule;

import com.fasterxml.classmate.TypeResolver;

import org.activiti.cloud.common.swagger.DocketCustomizer;
import org.activiti.cloud.services.query.rest.VariableSearch;
import org.springframework.core.Ordered;

import springfox.documentation.spring.web.plugins.Docket;

public class VariableSearchDocketCustomizer implements DocketCustomizer {

    private TypeResolver typeResolver;

    public VariableSearchDocketCustomizer(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Override
    public Docket customize(Docket docket) {
        return docket.alternateTypeRules(
                newRule(
                        typeResolver.resolve(VariableSearch.class),
                        typeResolver.resolve(VariableSearchWrapperMixin.class),
                        Ordered.HIGHEST_PRECEDENCE));
    }

    // the only purpose of this class is customizing the name of the parameters in the
    // swagger API in the places where  VariableSearch is used
    private static class VariableSearchWrapperMixin {

        private static class VariableSearchMixin {

            private String name;
            private String value;
            private String type;

            public String getName() {
                return name;
            }

            public String getValue() {
                return value;
            }

            public String getType() {
                return type;
            }
        }

        private VariableSearchMixin variables;

        public VariableSearchMixin getVariables() {
            return variables;
        }
    }
}
