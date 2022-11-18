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
package org.activiti.cloud.acc.core.helper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

public class SvgToPng {

    public static byte[] svgToPng(byte[] streamBytes) throws TranscoderException, IOException {
        try (
            ByteArrayInputStream input = new ByteArrayInputStream(streamBytes);
            ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            new PNGTranscoder().transcode(new TranscoderInput(input), new TranscoderOutput(output));
            output.flush();
            return output.toByteArray();
        }
    }
}
