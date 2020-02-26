/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.alfresco.config;

import java.util.ArrayList;
import java.util.List;

import org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageArgumentMethodResolver;
import org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageParameterParser;
import org.activiti.cloud.alfresco.converter.json.AlfrescoJackson2HttpMessageConverter;
import org.activiti.cloud.alfresco.converter.json.PageMetadataConverter;
import org.activiti.cloud.alfresco.converter.json.PagedResourcesConverter;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.alfresco.data.domain.ExtendedPageMetadataConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UriComponents;

@Configuration
@PropertySource("classpath:config/alfresco-rest-config.properties")
public class AlfrescoWebAutoConfiguration implements WebMvcConfigurer {

    private final PageableHandlerMethodArgumentResolver pageableHandlerMethodArgumentResolver;
    private final int defaultPageSize;

    public AlfrescoWebAutoConfiguration(PageableHandlerMethodArgumentResolver pageableHandlerMethodArgumentResolver,
                                        @Value("${spring.data.rest.default-page-size:100}") int defaultPageSize) {
        this.pageableHandlerMethodArgumentResolver = pageableHandlerMethodArgumentResolver;
        this.defaultPageSize = defaultPageSize;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(0, new AlfrescoPageArgumentMethodResolver(new AlfrescoPageParameterParser(defaultPageSize), pageableHandlerMethodArgumentResolver));
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        //the property spring.hateoas.use-hal-as-default-json-media-type is not working
        //we need to manually remove application/json from supported mediaTypes
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof TypeConstrainedMappingJackson2HttpMessageConverter ) {
                ArrayList<MediaType> mediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
                mediaTypes.remove(MediaType.APPLICATION_JSON);
                ((TypeConstrainedMappingJackson2HttpMessageConverter) converter).setSupportedMediaTypes(mediaTypes);
            }
        }

    }

    @Bean
    public <T> AlfrescoJackson2HttpMessageConverter<T> alfrescoJackson2HttpMessageConverter() {
        return new AlfrescoJackson2HttpMessageConverter<>(new PagedResourcesConverter(new PageMetadataConverter()));
    }

    @Bean
    public ExtendedPageMetadataConverter extendedPageMetadataConverter(){
        return new ExtendedPageMetadataConverter();
    }

    @Bean
    public <T> AlfrescoPagedResourcesAssembler<T> alfrescoPagedResourcesAssembler(@Autowired(required = false) HateoasPageableHandlerMethodArgumentResolver resolver,
                                                                                  @Autowired(required = false) UriComponents baseUri,
                                                                                  ExtendedPageMetadataConverter extendedPageMetadataConverter){
        return new AlfrescoPagedResourcesAssembler<>(resolver, baseUri, extendedPageMetadataConverter);
    }

}
