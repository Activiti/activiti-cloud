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

package org.activiti.cloud.common.swagger;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import springfox.documentation.oas.web.OpenApiTransformationContext;
import springfox.documentation.spi.DocumentationType;

@ExtendWith(MockitoExtension.class)
public class PathPrefixTransformationFilterTest {

    private PathPrefixTransformationFilter transformationFilter = new PathPrefixTransformationFilter("/base");

    @Mock
    private OpenApiTransformationContext<HttpServletRequest> context;

    @Test
    public void transform_should_createServerWithBasePathAndServicePrefix() {
        //given
        final OpenAPI openAPI = buildOpenAPI("/service");

        //when
        transformationFilter.transform(context);

        //then
        assertThat(openAPI.getServers()).extracting(Server::getUrl).containsExactly("/base/service");
    }

    @Test
    public void transform_should_createServerWithBasePathOnly_when_servicePrefixIsNotSet() {
        //given
        final OpenAPI openAPI = buildOpenAPI(null);

        //when
        transformationFilter.transform(context);

        //then
        assertThat(openAPI.getServers()).extracting(Server::getUrl).containsExactly("/base");
    }

    private OpenAPI buildOpenAPI(String servicePrefix) {
        final OpenAPI openAPI = new OpenAPI();
        openAPI.setExtensions(Collections.singletonMap(SwaggerDocketBuilder.SERVICE_URL_PREFIX,
            servicePrefix));
        given(context.getSpecification()).willReturn(openAPI);
        return openAPI;
    }

    @Test
    public void transform_should_convertDoubleSlashToSingleSlash_when_concatenationResultsInDoubleSlash() {
        //given
        PathPrefixTransformationFilter transformationFilter = new PathPrefixTransformationFilter("/");
        final OpenAPI openAPI = buildOpenAPI("/service");

        //when
        transformationFilter.transform(context);

        //then
        assertThat(openAPI.getServers()).extracting(Server::getUrl).containsExactly("/service");
    }

    @Test
    public void should_supportOAS3() {
        assertThat(transformationFilter.supports(DocumentationType.OAS_30)).isTrue();
    }

    @Test
    public void shouldNot_supportSwagger2() {
        assertThat(transformationFilter.supports(DocumentationType.SWAGGER_2)).isFalse();
    }
}
