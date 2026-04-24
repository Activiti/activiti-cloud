/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.acc.shared.rest.feign;

import feign.Response;
import feign.Util;
import feign.codec.Decoder;
import java.io.IOException;
import java.lang.reflect.Type;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.hateoas.mediatype.hal.HalJacksonModule;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * HAL decoder using Jackson 3
 */
public class HalDecoder extends ResponseEntityDecoder {

    public HalDecoder() {
        this(JsonMapper.builder().addModule(new HalJacksonModule()).build());
    }

    public HalDecoder(JsonMapper mapper) {
        super(new Jackson3Decoder(mapper));
    }

    private static class Jackson3Decoder implements Decoder {

        private final JsonMapper mapper;

        Jackson3Decoder(JsonMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public Object decode(Response response, Type type) throws IOException {
            if (response.body() == null) {
                return null;
            }
            if (String.class.equals(type)) {
                return Util.toString(response.body().asReader(Util.UTF_8));
            }
            JavaType javaType = mapper.constructType(type);
            return mapper.readValue(response.body().asInputStream(), javaType);
        }
    }
}
