package org.activiti.cloud.common.messaging.outbox;

import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.scheduling.PollSkipStrategy;

public class MetadataStorePollSkipStrategy implements PollSkipStrategy {

    public static final String SKIP_POLL = "false";
    private final ConcurrentMetadataStore metadataStore;
    private final String pollerKey;

    public MetadataStorePollSkipStrategy(ConcurrentMetadataStore metadataStore, String pollerKey) {
        this.metadataStore = metadataStore;
        this.pollerKey = pollerKey;
    }

    @Override
    public boolean skipPoll() {
        return SKIP_POLL.equalsIgnoreCase(metadataStore.get(pollerKey));
    }
}
