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
package org.activiti.cloud.common.messaging.functional;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.messaging.Message;

public class ConditionalMessageRoutingCallback implements MessageRoutingCallback {

    private final Map<String, Expression> routingTable;
    private final ExpressionParser expressionParser;


    public ConditionalMessageRoutingCallback() {
        this.expressionParser = new SpelExpressionParser();
        this.routingTable = new HashMap<>();
    }

    public void addRoutingExpression(String beanName, String condition) {
        routingTable.put(beanName, expressionParser.parseExpression(condition));
    }

    @Override
    public FunctionRoutingResult routingResult(Message<?> message) {
        String destination = routingTable.entrySet().stream()
            .filter(e -> e.getValue().getValue(message, Boolean.class))
            .map(Entry::getKey)
            .findFirst().orElse(null);


        return new FunctionRoutingResult(destination);
    }


}
