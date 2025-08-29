package org.activiti.cloud.services.job.executor;

import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.SubscribableChannel;

public interface AsyncExecutorJobMessageChannels {
    @InputBinding(MessageBasedJobManagerChannelsConstants.INPUT)
    default SubscribableChannel asyncExecutorJobsInput() {
        return MessageChannels.publishSubscribe().getObject();
    }

    @OutputBinding(MessageBasedJobManagerChannelsConstants.OUTPUT)
    default DirectChannel asyncExecutorJobsOutput() {
        return MessageChannels.direct().getObject();
    }
}
