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

package org.activiti.cloud.services.common.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ImageUtilsTest {

    @Test
    void should_convertSvgToPng() throws Exception {
        byte[] svg = this.getClass().getClassLoader().getResourceAsStream("images/save.svg").readAllBytes();
        byte[] png = ImageUtils.svgToPng(svg);
        byte[] expectedPng = this.getClass().getClassLoader().getResourceAsStream("images/save.png").readAllBytes();
        Assertions.assertThat(png).isEqualTo(expectedPng);
    }
}
