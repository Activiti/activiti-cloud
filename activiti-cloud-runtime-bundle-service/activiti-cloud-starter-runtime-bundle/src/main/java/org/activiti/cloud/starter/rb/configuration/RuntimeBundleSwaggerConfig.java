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
package org.activiti.cloud.starter.rb.configuration;

import com.fasterxml.classmate.TypeResolver;
import java.util.List;
import org.activiti.cloud.common.swagger.BaseAPIInfoBuilder;
import org.activiti.cloud.common.swagger.DocketCustomizer;
import org.activiti.cloud.common.swagger.SwaggerDocketBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
public class RuntimeBundleSwaggerConfig {

    @Bean(name = "rbApiDocket")
    @ConditionalOnMissingBean(name = "rbApiDocket")
    public Docket rbApiDocket(BaseAPIInfoBuilder baseAPIInfoBuilder, TypeResolver typeResolver,
       List<DocketCustomizer> docketCustomizers,
        @Value("${activiti.cloud.swagger.rb-base-path:}") String rbSwaggerBasePath) {
        return new SwaggerDocketBuilder("org.activiti.cloud.services.rest",
            typeResolver, docketCustomizers)
            .buildApiDocket("Runtime Bundle", rbSwaggerBasePath)
            .apiInfo(baseAPIInfoBuilder.baseApiInfoBuilder("Runtime Bundle ReST API")
                .build());
    }

    @Bean
    @ConditionalOnMissingBean
    public PayloadsDocketCustomizer payloadsDocketCustomizer() {
        return new PayloadsDocketCustomizer();
    }

}
