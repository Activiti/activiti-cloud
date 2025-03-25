package org.activiti.services.connectors.enricher;

import org.activiti.api.process.model.IntegrationContext;

public interface IntegrationContextEnricher {
    void enrich(IntegrationContext integrationContext);
}
