/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.common.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.metadata.ConcurrentMetadataStore;

@ExtendWith(MockitoExtension.class)
class MetadataStorePollSkipStrategyTest {

    private static final String POLLER_KEY = "test.poller.enabled";

    @Mock
    private ConcurrentMetadataStore metadataStore;

    private MetadataStorePollSkipStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new MetadataStorePollSkipStrategy(metadataStore, POLLER_KEY);
    }

    @ParameterizedTest
    @ValueSource(strings = { "false", "FALSE", "False", "fAlSe" })
    @DisplayName("Should skip poll when metadata store value is 'false' regardless of case")
    void skipPoll_whenValueIsFalseIgnoreCase_returnsTrue(String storeValue) {
        when(metadataStore.get(POLLER_KEY)).thenReturn(storeValue);

        boolean result = strategy.skipPoll();

        assertThat(result).isTrue();
        verify(metadataStore).get(POLLER_KEY);
    }

    @ParameterizedTest
    @ValueSource(strings = { "true", "TRUE", "yes", "1", "", "   " })
    @DisplayName("Should not skip poll when metadata store value is anything other than 'false'")
    void skipPoll_whenValueIsNotFalse_returnsFalse(String storeValue) {
        when(metadataStore.get(POLLER_KEY)).thenReturn(storeValue);

        boolean result = strategy.skipPoll();

        assertThat(result).isFalse();
        verify(metadataStore).get(POLLER_KEY);
    }

    @Test
    @DisplayName("Should not skip poll when metadata store returns null")
    void skipPoll_whenValueIsNull_returnsFalse() {
        when(metadataStore.get(POLLER_KEY)).thenReturn(null);

        boolean result = strategy.skipPoll();

        assertThat(result).isFalse();
        verify(metadataStore).get(POLLER_KEY);
    }
}
