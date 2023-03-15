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

package org.activiti.cloud.modeling.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import java.io.IOException;
import java.util.Base64;
import org.activiti.cloud.services.common.util.ImageProcessingException;
import org.activiti.cloud.services.common.util.ImageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringSanitizingDeserializer extends StringDeserializer {

    private static final long serialVersionUID = -5484119319853721838L;

    private static final Logger logger = LoggerFactory.getLogger(StringSanitizingDeserializer.class);

    private static final String SVG_IMAGE_PREFIX = "data:image/svg+xml;base64,";

    private static final String SVG_IMAGE_PREFIX_REGEX = "^data:image/svg\\+xml;base64,";

    private static final String PNG_IMAGE_PREFIX = "data:image/png;base64,";

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.hasToken(JsonToken.VALUE_STRING) && p.getText().trim().toLowerCase().startsWith(SVG_IMAGE_PREFIX)) {
            final String deserializedString = convertSvgToPng(p.getText());
            if (deserializedString != null) {
                return deserializedString;
            }
        }
        return super.deserialize(p, ctxt);
    }

    private String convertSvgToPng(final String svgImageText) throws IOException {
        try {
            final String base64Svg = svgImageText.replaceFirst(SVG_IMAGE_PREFIX_REGEX, "");
            final byte[] decodedSvgBytes = Base64.getDecoder().decode(base64Svg);
            final byte[] pngBytes = ImageUtils.svgToPng(decodedSvgBytes);
            final String base64Png = Base64.getEncoder().encodeToString(pngBytes);
            return PNG_IMAGE_PREFIX + base64Png;
        } catch (ImageProcessingException e) {
            logger.warn("Image processing error", e);
        }
        return null;
    }
}
