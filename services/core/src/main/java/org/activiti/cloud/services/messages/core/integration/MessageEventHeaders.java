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
package org.activiti.cloud.services.messages.core.integration;

public final class MessageEventHeaders {

    public final static String APP_NAME = "appName"; 
    public final static String APP_VERSION = "appVersion";
    public final static String SERVICE_NAME = "serviceName";
    public final static String SERVICE_FULL_NAME = "serviceFullName";
    public final static String SERVICE_TYPE = "serviceType";
    public final static String SERVICE_VERSION = "serviceVersion";
    
    public static final String MESSAGE_EVENT_ID = "messageEventId";
    public static final String MESSAGE_EVENT_CORRELATION_KEY = "messageEventCorrelationKey";
    public static final String MESSAGE_EVENT_BUSINESS_KEY = "messageEventBusinessKey";
    public static final String MESSAGE_EVENT_NAME = "messageEventName";
    public static final String MESSAGE_EVENT_TYPE = "messageEventType";
    public static final String MESSAGE_PAYLOAD_TYPE = "messagePayloadType"; 

}