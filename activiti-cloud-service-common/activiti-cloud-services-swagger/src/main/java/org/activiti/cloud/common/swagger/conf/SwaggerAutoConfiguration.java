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
package org.activiti.cloud.common.swagger.conf;

import com.fasterxml.classmate.TypeResolver;
import org.activiti.cloud.common.swagger.BaseAPIInfoBuilder;
import org.activiti.cloud.common.swagger.SwaggerOperationIdTrimmer;
import org.activiti.cloud.common.swagger.DocketCustomizer;
import org.activiti.cloud.common.swagger.PathPrefixTransformationFilter;
import org.activiti.cloud.common.swagger.SwaggerDocketBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.RequestHandler;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.spring.web.plugins.WebFluxRequestHandlerProvider;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Predicate;

/**
 * Provides base springfox configuration for swagger auto-generated specification file. It provides two
 * swagger specification files: the default one is available under `v3/api-docs`
 * and provides specification for Alfresco MediaType format
 *
 * This configuration is not self-contained: the one adding this as dependency should provide a bean of type
 * {@link Predicate<RequestHandler>} that will be injected under {@link Docket#select()}. I.e
 * <code>test</code>
 * {@code test}
 * <pre>
 *     &#64;Bean
 *     public Predicate&#60;RequestHandler&#62; apiSelector() {
 *         return RequestHandlerSelectors.basePackage("org.activiti.cloud.services");
 *     }
 *  </pre>
 *
 */
@Configuration
@EnableOpenApi
@PropertySource("classpath:swagger-config.properties")
@ConditionalOnProperty(value = "springfox.enabled", havingValue = "true", matchIfMissing = true)
public class SwaggerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SwaggerDocketBuilder swaggerDocketBuilder(BuildProperties buildProperties, TypeResolver typeResolver,
        List<DocketCustomizer> docketCustomizers) {
        return new SwaggerDocketBuilder(new BaseAPIInfoBuilder(buildProperties), typeResolver, docketCustomizers);
    }

    @Bean
    @Primary
    public SwaggerOperationIdTrimmer customOperationNameGenerator() {
        return new SwaggerOperationIdTrimmer();
    }

    @Bean
    @ConditionalOnMissingBean
    public PathPrefixTransformationFilter pathPrefixTransformationFilter(@Value("${activiti.cloud.swagger.base-path:/}") String swaggerBasePath) {
        return new PathPrefixTransformationFilter(swaggerBasePath);
    }

    /**
     * Springfox workaround required by Spring Boot 2.6
     * See https://github.com/springfox/springfox/issues/346
     */
    @Bean
    public static BeanPostProcessor springfoxHandlerProviderBeanPostProcessor() {
        return new BeanPostProcessor() {

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof WebMvcRequestHandlerProvider || bean instanceof WebFluxRequestHandlerProvider) {
                    customizeSpringfoxHandlerMappings(getHandlerMappings(bean));
                }
                return bean;
            }

            private <T extends RequestMappingInfoHandlerMapping> void customizeSpringfoxHandlerMappings(List<T> mappings) {
                mappings.removeIf(mapping -> mapping.getPatternParser() != null);
            }

            @SuppressWarnings("unchecked")
            private List<RequestMappingInfoHandlerMapping> getHandlerMappings(Object bean) {
                try {
                    Field field = ReflectionUtils.findField(bean.getClass(), "handlerMappings");
                    field.setAccessible(true);
                    return (List<RequestMappingInfoHandlerMapping>) field.get(bean);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

}
