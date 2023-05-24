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

package org.activiti.cloud.alfresco.filter;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class TrailingSlashRedirectFilterTest {

    private static final String BASEURL = "/v1";

    @Autowired
    MockMvc mvc;

    @SpringBootApplication
    static class TestApplication {}

    @Test
    void testGreeting() throws Exception {
        mvc
            .perform(get(BASEURL + "/greeting").accept(APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(content().string("Hello, World!"));
    }

    @Test
    void testGreetingWithRequestParams() throws Exception {
        mvc
            .perform(get(BASEURL + "/greeting?query=Foo").accept(APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(content().string("Hello, Foo!"));
    }

    @Test
    void testGreetingTrailingSlashWithFilter() throws Exception {
        mvc
            .perform(get(BASEURL + "/greeting/").accept(APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(content().string("Hello, World!"));
    }

    @Test
    void testGreetingTrailingSlashAndRequestQueryParameter() throws Exception {
        mvc
            .perform(get(BASEURL + "/greeting/?query=Bar").accept(APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(content().string("Hello, Bar!"));
    }

    @Test
    void testGreetingTrailingSlash() throws Exception {
        mvc.perform(get(BASEURL + "/greeting/slash/").accept(APPLICATION_JSON_VALUE)).andExpect(status().isNotFound());
    }
}
