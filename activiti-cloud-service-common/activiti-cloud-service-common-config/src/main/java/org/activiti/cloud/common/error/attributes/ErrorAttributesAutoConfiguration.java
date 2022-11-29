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

package org.activiti.cloud.common.error.attributes;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Map;

@Configuration
public class ErrorAttributesAutoConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ErrorAttributes errorAttributes(List<ErrorAttributesCustomizer> errorAttributesCustomizers) {
        return new DefaultErrorAttributes() {

            @Override
            public Map<String, Object> getErrorAttributes(WebRequest webRequest,
                                                          ErrorAttributeOptions options) {
                Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest,
                    options);

                for (ErrorAttributesCustomizer customizer : errorAttributesCustomizers) {
                    errorAttributes = customizer.customize(errorAttributes, getError(webRequest));
                }

                return errorAttributes;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(name="errorAttributesMessageSanitizer")
    public ErrorAttributesCustomizer errorAttributesMessageSanitizer() {
        return new ErrorAttributesMessageSanitizer();
    }
}

