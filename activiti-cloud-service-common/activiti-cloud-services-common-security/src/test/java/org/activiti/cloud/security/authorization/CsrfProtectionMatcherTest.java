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
package org.activiti.cloud.security.authorization;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class CsrfProtectionMatcherTest {

    List<String> publicUrlsPatterns = asList("/public", "/public/**");
    List<String> nonPublicUrlsPatterns = asList("/non-public", "/non-public/**");

    @Test
    void should_matchNonPublicURLsPatterns() {
        CsrfProtectionMatcher matcher = new CsrfProtectionMatcher(publicUrlsPatterns);
        nonPublicUrlsPatterns.forEach(url -> assertThat(matcher.matches(new MockHttpServletRequest("", url))).isTrue());
    }

    @Test
    void should_not_matchPublicURLsPatterns() {
        CsrfProtectionMatcher matcher = new CsrfProtectionMatcher(publicUrlsPatterns);
        publicUrlsPatterns.forEach(url -> assertThat(matcher.matches(new MockHttpServletRequest("", url))).isFalse());
    }
}
