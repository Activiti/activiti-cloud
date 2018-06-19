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

package org.activiti.cloud.services.query.events.handlers;

/**
 * Tests for ProcessCancelledEventHandler
 */
public class ProcessCancelledEventHandlerTest {

//    @InjectMocks
//    private ProcessCancelledEventHandler handler;
//
//    @Mock
//    private ProcessInstanceRepository processInstanceRepository;
//
//    @Rule
//    public ExpectedException expectedException = ExpectedException.none();
//
//    @Before
//    public void setUp() throws Exception {
//        initMocks(this);
//    }
//
//    /**
//     * Test that ProcessCancelledEventHandler updates the existing process instance as following:
//     * - status to CANCELLED
//     * - lastModified to the event time
//     */
//    @Test
//    public void testUpdateExistingProcessInstanceWhenCancelled() throws Exception {
//        //given
//        Long eventTime = System.currentTimeMillis();
//        ProcessInstance processInstance = mock(ProcessInstance.class);
//        given(processInstanceRepository.findById("200")).willReturn(Optional.of(processInstance));
//
//        //when
//        handler.handle(createProcessCancelledEvent("200",
//                                                   eventTime));
//
//        //then
//        verify(processInstanceRepository).save(processInstance);
//        verify(processInstance).setStatus("CANCELLED");
//        verify(processInstance).setLastModified(eq(new Date(eventTime)));
//    }
//
//    /**
//     * Test that ProcessCancelledEventHandler throws ActivitiException when the related process instance is not found
//     */
//    @Test
//    public void testThrowExceptionWhenProcessInstanceNotFound() throws Exception {
//        //given
//        given(processInstanceRepository.findById("200")).willReturn(Optional.empty());
//
//        //then
//        expectedException.expect(ActivitiException.class);
//        expectedException.expectMessage("Unable to find process instance with the given id: ");
//
//        //when
//        handler.handle(createProcessCancelledEvent("200"));
//    }
//
//    /**
//     * Test that ProcessCancelledEventHandler is handling ProcessCancelledEvent events
//     */
//    @Test
//    public void testHandleProcessCancelledEvent() {
//        //when
//        Class<? extends ProcessEngineEvent> handledEventClass = handler.getHandledEventClass();
//
//        //then
//        assertThat(handledEventClass).isEqualTo(ProcessCancelledEvent.class);
//    }
}