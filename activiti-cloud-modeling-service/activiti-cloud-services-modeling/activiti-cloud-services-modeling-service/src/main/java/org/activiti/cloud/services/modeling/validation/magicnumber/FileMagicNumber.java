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
package org.activiti.cloud.services.modeling.validation.magicnumber;

import java.math.BigInteger;
import java.util.Arrays;

public class FileMagicNumber {

    private String name;
    private String string;
    private byte[] bytes;
    private int offset;

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public boolean accept(byte[] fileContent) {
        if (bytes == null && string != null) {
            this.calculateBytesFromString(string);
        }
        boolean result = false;
        if (fileContent.length >= bytes.length + offset) {
            result = Arrays.compare(fileContent, offset, bytes.length, bytes, 0, bytes.length) == 0;
        }
        return result;
    }

    /**
     * Please make attention, in order to avoid to re-calculate
     * the byte array every time it is "cached" in the bytes variable
     * @param string
     */
    private void calculateBytesFromString(String string) {
        byte[] stringBytes;
        if (string.startsWith("0x")) {
            String hexString = string.substring(2);
            int i = Integer.parseUnsignedInt(hexString, 16);
            stringBytes = BigInteger.valueOf(i).toByteArray();
        } else {
            stringBytes = string.getBytes();
        }
        this.setBytes(stringBytes);
    }

}
