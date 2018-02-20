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

package org.activiti.cloud.qa.modeling;

import java.util.List;

import feign.Feign;
import feign.Headers;
import feign.Logger;
import feign.Param;
import feign.RequestInterceptor;
import feign.RequestLine;
import feign.RequestTemplate;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import net.serenitybdd.core.Serenity;
import org.activiti.cloud.qa.Config;
import org.activiti.cloud.qa.modeling.model.Group;

/**
 * Modeling REST client
 */
public interface ModelingClient {

    @RequestLine("GET")
    @Headers("Content-Type: application/json")
    List<Group> findAll();

    @RequestLine("GET /{id}")
    @Headers("Content-Type: application/json")
    Group findById(@Param("id") String id);

    @RequestLine("POST")
    @Headers("Content-Type: application/json")
    void create(Group group);

    static ModelingClient get() {
        return Feign.builder()
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .logger(new Logger.ErrorLogger())
                .logLevel(Logger.Level.FULL)
                .requestInterceptor(new OAuth2FeignRequestInterceptor())
                .target(ModelingClient.class,
                        Config.getInstance().getProperties().getProperty("modeling.groups.url"));
    }

    class OAuth2FeignRequestInterceptor implements RequestInterceptor {

        public static final String BEARER = "Bearer";

        public static final String AUTHORIZATION = "Authorization";

        @Override
        public void apply(RequestTemplate template) {
            template.header(AUTHORIZATION,
                            String.format("%s %s",
                                          BEARER,
                                          Serenity.sessionVariableCalled("authToken")));
        }
    }

}
