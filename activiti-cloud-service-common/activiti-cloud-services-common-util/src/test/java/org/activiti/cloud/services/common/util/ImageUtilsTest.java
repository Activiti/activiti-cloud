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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ImageUtilsTest {

    private static final String SVG_FILE_LOCATION = "images/save.svg";

    private static final String PNG_FILE_LOCATION = "images/save.png";

    @Test
    void should_convertSvgToPng() throws Exception {
        byte[] svg = FileUtils.resourceAsStream(SVG_FILE_LOCATION)
                .orElseThrow(() -> new IllegalArgumentException(SVG_FILE_LOCATION + " file not found"))
                .readAllBytes();
        byte[] png = ImageUtils.svgToPng(svg);
        byte[] expectedPng = FileUtils.resourceAsStream(PNG_FILE_LOCATION)
                .orElseThrow(() -> new IllegalArgumentException(PNG_FILE_LOCATION + " file not found"))
                .readAllBytes();
        assertThat(png).isEqualTo(expectedPng);
    }

    @Test
    void should_throwImageProcessingException_when_TranscoderExceptionThrown() {
        byte[] svg = "wrong svg".getBytes(StandardCharsets.UTF_8);
        assertThatExceptionOfType(ImageProcessingException.class).isThrownBy(() -> ImageUtils.svgToPng(svg));
    }
}
