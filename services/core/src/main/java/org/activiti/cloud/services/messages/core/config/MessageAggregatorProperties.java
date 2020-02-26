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

package org.activiti.cloud.services.messages.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;

/**
 * Configuration properties for the Messages Service.
 * 
 */
@ConfigurationProperties(MessageAggregatorProperties.PREFIX)
public class MessageAggregatorProperties {

    static final String PREFIX = "activiti.cloud.services.messages";

    /**
     * SpEL expression for timeout to expiring uncompleted groups
     */
    private Expression groupTimeout;

    /**
     * Persistence message store entity: table prefix in RDBMS, collection name in MongoDb, etc
     */
    private String messageStoreEntity;
    

    public Expression getGroupTimeout() {
        return this.groupTimeout;
    }

    public void setGroupTimeout(Expression groupTimeout) {
        this.groupTimeout = groupTimeout;
    }

    public String getMessageStoreEntity() {
        return this.messageStoreEntity;
    }

    public void setMessageStoreEntity(String messageStoreEntity) {
        this.messageStoreEntity = messageStoreEntity;
    }

}