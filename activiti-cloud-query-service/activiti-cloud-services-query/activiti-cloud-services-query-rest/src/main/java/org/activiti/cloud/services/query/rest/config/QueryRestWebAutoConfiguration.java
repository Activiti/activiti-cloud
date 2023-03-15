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

package org.activiti.cloud.services.query.rest.config;

import java.util.List;
import org.activiti.cloud.services.query.rest.VariableSearchArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class QueryRestWebAutoConfiguration implements WebMvcConfigurer {

    private ConversionService conversionService;

    public QueryRestWebAutoConfiguration(@Lazy ConversionService mvcConversionService) {
        this.conversionService = mvcConversionService;
    }

    @Override
    public void addArgumentResolvers(@Lazy List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new VariableSearchArgumentResolver(conversionService));
    }
}
