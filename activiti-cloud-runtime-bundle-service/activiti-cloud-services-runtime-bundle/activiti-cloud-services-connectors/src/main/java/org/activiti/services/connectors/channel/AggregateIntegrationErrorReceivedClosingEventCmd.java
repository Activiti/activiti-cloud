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

package org.activiti.services.connectors.channel;

import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;

class AggregateIntegrationErrorReceivedClosingEventCmd
    extends CommandContextCloseListenerAdapter
    implements Command<Void> {

    private final AggregateIntegrationErrorReceivedEventCmd aggregateIntegrationErrorReceivedEventCmd;

    AggregateIntegrationErrorReceivedClosingEventCmd(
        AggregateIntegrationErrorReceivedEventCmd aggregateIntegrationErrorReceivedEventCmd
    ) {
        this.aggregateIntegrationErrorReceivedEventCmd = aggregateIntegrationErrorReceivedEventCmd;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        commandContext.addCloseListener(this);

        return null;
    }

    @Override
    public void closing(CommandContext commandContext) {
        aggregateIntegrationErrorReceivedEventCmd.execute(commandContext);
    }
}
