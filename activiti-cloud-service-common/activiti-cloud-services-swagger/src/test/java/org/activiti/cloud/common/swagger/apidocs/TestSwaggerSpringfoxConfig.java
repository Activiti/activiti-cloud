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
package org.activiti.cloud.common.swagger.apidocs;

import org.activiti.cloud.common.swagger.SwaggerDocketBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.function.Predicate;

@TestConfiguration
public class TestSwaggerSpringfoxConfig {

    @Bean(name = "APIDocket")
    public Docket alfrescoAPIDocket(SwaggerDocketBuilder docketBuilder) {
        return docketBuilder.buildApiDocket("Test ReST API", "testing",
            "/test", "org.activiti.cloud.common.swagger.apidocs");
    }

    @Bean
    public Predicate<RequestHandler> apiSelector() {
        return RequestHandlerSelectors.basePackage("org.activiti.cloud.common.swagger.apidocs");
    }

}
