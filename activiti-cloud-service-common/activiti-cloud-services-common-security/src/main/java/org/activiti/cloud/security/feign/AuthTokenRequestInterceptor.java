package org.activiti.cloud.security.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.util.Optional;

public interface AuthTokenRequestInterceptor extends RequestInterceptor {

    String AUTHORIZATION = "Authorization";
    String BEARER = "Bearer";

    Optional<String> getToken();

    @Override
    default void apply(RequestTemplate template) {
        getToken()
            .ifPresent(token -> {
                template.removeHeader(AUTHORIZATION);
                template.header(AUTHORIZATION,
                                String.format("%s %s",
                                              BEARER,
                                              token));
            });
    }

}
