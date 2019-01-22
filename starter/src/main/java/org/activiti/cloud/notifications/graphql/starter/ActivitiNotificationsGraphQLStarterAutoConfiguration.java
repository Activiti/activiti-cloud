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
