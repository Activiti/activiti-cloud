package org.activiti.cloud.api.events;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.api.process.model.events.BPMNActivityEvent;
import org.activiti.api.process.model.events.BPMNErrorReceivedEvent;
import org.activiti.api.process.model.events.BPMNMessageEvent;
import org.activiti.api.process.model.events.BPMNSignalEvent;
import org.activiti.api.process.model.events.BPMNTimerEvent;
import org.activiti.api.process.model.events.IntegrationEvent;
import org.activiti.api.process.model.events.MessageDefinitionEvent;
import org.activiti.api.process.model.events.MessageSubscriptionEvent;
import org.activiti.api.process.model.events.ProcessDefinitionEvent;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.process.model.events.SequenceFlowEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CloudRuntimeEventTypeTest {

    private <E extends Enum<E>> List<String> getEnumValuesAsStringList(Class<E> enumClass) {
        return EnumSet.allOf(enumClass).stream()
                .map(Enum::name).collect(Collectors.toList());
    }

    @Test
    public void shouldContainAllValueFromEventEnums() {
        List<String> enumValues = this.getEnumValuesAsStringList(CloudRuntimeEventType.class);

        assertThat(enumValues).containsAll(this.getEnumValuesAsStringList(BPMNActivityEvent.ActivityEvents.class));
        assertThat(enumValues).containsAll(this.getEnumValuesAsStringList(BPMNErrorReceivedEvent.ErrorEvents.class));
        assertThat(enumValues).containsAll(this.getEnumValuesAsStringList(BPMNSignalEvent.SignalEvents.class));
        assertThat(enumValues).containsAll(this.getEnumValuesAsStringList(ProcessDefinitionEvent.ProcessDefinitionEvents.class));
        assertThat(enumValues).containsAll(this.getEnumValuesAsStringList(IntegrationEvent.IntegrationEvents.class));
        assertThat(enumValues).containsAll(this.getEnumValuesAsStringList(BPMNTimerEvent.TimerEvents.class));
        assertThat(enumValues).containsAll(this.getEnumValuesAsStringList(BPMNMessageEvent.MessageEvents.class));
        assertThat(enumValues).containsAll(this.getEnumValuesAsStringList(ProcessDefinitionEvent.ProcessDefinitionEvents.class));
        assertThat(enumValues).containsAll(this.getEnumValuesAsStringList(SequenceFlowEvent.SequenceFlowEvents.class));
        assertThat(enumValues).containsAll(this.getEnumValuesAsStringList(ProcessRuntimeEvent.ProcessEvents.class));
        assertThat(enumValues).containsAll(this.getEnumValuesAsStringList(MessageDefinitionEvent.MessageDefinitionEvents.class));
        assertThat(enumValues).containsAll(this.getEnumValuesAsStringList(MessageSubscriptionEvent.MessageSubscriptionEvents.class));
    }

}
