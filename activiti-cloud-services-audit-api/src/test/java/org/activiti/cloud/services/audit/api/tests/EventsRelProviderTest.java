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

package org.activiti.cloud.services.audit.api.tests;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessStartedEvent;
import org.activiti.cloud.services.audit.api.resources.EventsRelProvider;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EventsRelProviderTest {

    private EventsRelProvider relProvider = new EventsRelProvider();

    @Test
    public void shouldSupportCloudRuntimeEvent() throws Exception {
        //given
        Class<CloudRuntimeEvent> aClass = CloudRuntimeEvent.class;

        //when
        boolean supports = relProvider.supports(aClass);

        //then
        assertThat(supports).isTrue();
    }

    @Test
    public void shouldSupportCloudRuntimeEventSubClasses() throws Exception {
        //given
        Class<CloudProcessStartedEvent> aClass = CloudProcessStartedEvent.class;

        //when
        boolean supports = relProvider.supports(aClass);

        //then
        assertThat(supports).isTrue();
    }

    @Test
    public void shouldNotSupportClassesOtherThanCloudRuntimeEventAndSubClasses() throws Exception {
        //given
        Class<String> aClass = String.class;

        //when
        boolean supports = relProvider.supports(aClass);

        //then
        assertThat(supports).isFalse();
    }

    @Test
    public void getCollectionResourceRelForShouldReturnLiteralEvents() throws Exception {
        //given
        Class<CloudRuntimeEvent> aClass = CloudRuntimeEvent.class;

        //when
        String collectionRel = relProvider.getCollectionResourceRelFor(aClass);

        //then
        assertThat(collectionRel).isEqualTo("events");
    }

    @Test
    public void getItemResourceRelForShouldReturnLiteralEvent() throws Exception {
        //given
        Class<CloudRuntimeEvent> aClass = CloudRuntimeEvent.class;

        //when
        String itemRel = relProvider.getItemResourceRelFor(aClass);

        //then
        assertThat(itemRel).isEqualTo("event");
    }
}