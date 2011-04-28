/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.util;

/**
 * Simple encoder of byte arrays into String array using only the hexadecimal alphabet
 */
public class HexEncoder {

    private static final char[] ALPHABET = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a',
            'b', 'c', 'd', 'e', 'f'};

    public static String encode(byte[] packet) {
        StringBuffer chars = new StringBuffer(16);
        for (int i = 0; i < packet.length; i++) {
            int highBits = (packet[i] & 0xF0) >> 4;
            int lowBits = packet[i] & 0x0F;
            chars.append(ALPHABET[highBits]);
            chars.append(ALPHABET[lowBits]);
        }
        return chars.toString();
    }

}
