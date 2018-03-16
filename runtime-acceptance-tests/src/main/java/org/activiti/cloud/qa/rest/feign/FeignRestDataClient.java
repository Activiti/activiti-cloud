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

package org.activiti.cloud.qa.rest.feign;

import java.util.List;
import java.util.Map;

import feign.Body;
import feign.Feign;
import feign.Headers;
import feign.Logger;
import feign.Param;
import feign.RequestLine;
import feign.gson.GsonEncoder;
import org.activiti.cloud.qa.service.BaseService;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;

/**
 * Generic REST client operations
 */
public interface FeignRestDataClient<C extends FeignRestDataClient, R> {

    @RequestLine("POST")
    @Headers("Content-Type: application/json")
    void create(R resource);

    @RequestLine("GET /{id}")
    @Headers("Content-Type: application/json")
    Resource<R> findById(@Param("id") String id);

    @RequestLine("GET")
    @Headers("Content-Type: application/json")
    PagedResources<Resource<R>> findAll();

    @RequestLine("PUT /{id}")
    @Headers("Content-Type: application/json")
    void updateById(@Param("id") String id, R resource);

    @RequestLine("PUT")
    @Headers("Content-Type: application/json")
    void update(R resource);

    @RequestLine("PUT")
    @Headers("Content-Type: text/uri-list")
    @Body("{uriList}")
    void addRelation(@Param(value = "uriList") String relationUri);

    @RequestLine("PUT")
    @Headers("Content-Type: text/uri-list")
    @Body("{uriList}")
    void addRelation(@Param(value = "uriList") List<String> relationUriList);

    @RequestLine("GET")
    @Headers("Content-Type: application/json")
    Resource<R> get();

    @RequestLine("DELETE")
    @Headers("Content-Type: application/json")
    void delete();

    Class<C> getType();

    default PagedResources<Resource<R>> findAllByUri(String uri) {
        return buildByUri(uri).findAll();
    }

    default Resource<R> findByUri(String uri) {
        return buildByUri(uri).get();
    }

    default void addRelationByUri(String uri,
                                  String relationUri) {
        buildByUri(uri).addRelation(relationUri);
    }

    default void addRelationByUri(String uri,
                                  List<String> relationUriList) {
        buildByUri(uri).addRelation(relationUriList);
    }

    default void updateByUri(String uri, R resource) {
        buildByUri(uri).update(resource);
    }

    default void deleteByUri(String uri) {
        buildByUri(uri).delete();
    }

    default C buildByUri(String uri) {
        return builder().target(getType(),
                                uri);
    }

    static Feign.Builder builder() {
        return Feign.builder()
                .encoder(new GsonEncoder())
                .decoder(new HalDecoder())
                .logger(new Logger.ErrorLogger())
                .logLevel(Logger.Level.FULL)
                .requestInterceptor(new OAuth2FeignRequestInterceptor());
    }

}
