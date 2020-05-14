/*
 * Copyright 2017-2020 Alfresco.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.notifications.graphql.starter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ActivitiNotificationsGraphQLStarterAutoConfiguration {
	
    @Configuration
    public class DefaultActivitiNotificationsGraphQLStarterConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnProperty(value="keycloak.cors", matchIfMissing = true)
		public WebMvcConfigurer graphQLBackendCorsConfiguration() {
			return new WebMvcConfigurer() {
				@Override
				public void addCorsMappings(CorsRegistry registry) {
					registry.addMapping("/**")
							.allowedOrigins("*")
							.allowedMethods(HttpMethod.GET.toString(), 
										    HttpMethod.POST.toString(),
										    HttpMethod.PUT.toString(), 
										    HttpMethod.DELETE.toString(), 
										    HttpMethod.OPTIONS.toString());
				}
			};
		}
    }    	

}
