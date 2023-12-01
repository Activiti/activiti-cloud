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
package org.activiti.cloud.services.events.listeners;

import java.util.ArrayList;
import java.util.List;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandContextCloseListener;

public abstract class BaseCommandContextEventsAggregator<E, L extends CommandContextCloseListener> {

    public void add(E element) {
        CommandContext currentCommandContext = getCurrentCommandContext();
        List<E> attributes = currentCommandContext.getGenericAttribute(getAttributeKey());
        if (attributes == null) {
            attributes = new ArrayList<>();
            currentCommandContext.addAttribute(getAttributeKey(), attributes);
        }
        attributes.add(element);

        if (!currentCommandContext.hasCloseListener(getCloseListenerClass())) {
            currentCommandContext.addCloseListener(getCloseListener());
        }
    }

    protected abstract Class<L> getCloseListenerClass();

    protected abstract L getCloseListener();

    protected abstract String getAttributeKey();

    protected CommandContext getCurrentCommandContext() {
        return Context.getCommandContext();
    }
}
