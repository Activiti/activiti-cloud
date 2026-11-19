package org.activiti.cloud.services.audit.jpa.streams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.persistence.EntityManager;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.cloud.services.audit.jpa.streams.config.AuditJPAStreamsAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AuditPayloadCleanupConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(AuditJPAStreamsAutoConfiguration.class)
        .withBean(EntityManager.class, () -> mock(EntityManager.class))
        .withBean(EventsRepository.class, () -> mock(EventsRepository.class))
        .withBean(APIEventToEntityConverters.class, () -> mock(APIEventToEntityConverters.class));

    @Test
    void should_preserveExistingRetention_whenPolicyIsAbsent() {
        runner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(AuditPayloadCleanup.class));
    }

    @ParameterizedTest
    @ValueSource(strings = { "keep", "transient" })
    void should_selectCleanupByPolicy(String policy) {
        runner.withPropertyValues("activiti.cloud.processing-data.retention-policy=" + policy).run(context -> {
            assertThat(context).hasNotFailed();
            if (policy.equals("transient")) {
                assertThat(context).hasSingleBean(AuditPayloadCleanup.class);
                assertThat(context.getBean(AuditPayloadCleanup.class)).isInstanceOf(TransientAuditPayloadCleanup.class);
            } else {
                assertThat(context).doesNotHaveBean(AuditPayloadCleanup.class);
            }
        });
    }

    @Test
    void should_allowReplacementStrategy() {
        var replacement = mock(AuditPayloadCleanup.class);
        runner
            .withPropertyValues("activiti.cloud.processing-data.retention-policy=transient")
            .withBean("replacementCleanup", AuditPayloadCleanup.class, () -> replacement)
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(AuditPayloadCleanup.class);
                assertThat(context.getBean(AuditPayloadCleanup.class)).isSameAs(replacement);
            });
    }
}
