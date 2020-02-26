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

package org.activiti.cloud.acc.shared.rest.feign;

import java.util.List;

import feign.Body;
import feign.Feign;
import feign.Headers;
import feign.Logger;
import feign.Param;
import feign.RequestLine;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.gson.GsonEncoder;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;

/**
 * Generic REST client operations
 */
public interface FeignRestDataClient<C extends FeignRestDataClient, R> {

    @RequestLine("POST")
    @Headers("Content-Type: application/json")
    Resource<R> create(R resource);

    @RequestLine("GET /{id}")
    @Headers("Content-Type: application/json")
    Resource<R> findById(@Param("id") String id);

    @RequestLine("GET")
    @Headers("Content-Type: application/json")
    PagedResources<Resource<R>> findAll();

    @RequestLine("PUT /{id}")
    @Headers("Content-Type: application/json")
    void updateById(@Param("id") String id,
                    R resource);

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

    default Resource<R> createByUri(String uri,
                             R resource) {
        return buildByUri(uri).create(resource);
    }

    default void updateByUri(String uri,
                             R resource) {
        buildByUri(uri).update(resource);
    }

    default void deleteByUri(String uri) {
        buildByUri(uri).delete();
    }

    default C buildByUri(String uri) {
        return builder().target(getType(),
                                uri);
    }

    default Encoder encoder() {
        return new GsonEncoder();
    }

    default Decoder decoder() {
        return new HalDecoder();
    }

    default Feign.Builder builder() {
        return builder(encoder(),
                       decoder());
    }

    static Feign.Builder builder(Encoder encoder,
                                 Decoder decoder) {
        return Feign.builder()
                .encoder(encoder)
                .decoder(decoder)
                .errorDecoder(new FeignErrorDecoder())
                .logger(new Logger.ErrorLogger())
                .logLevel(Logger.Level.FULL)
                .requestInterceptor(new OAuth2FeignRequestInterceptor());
    }
}
