/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.directory.digest;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * @since 7.1
 */
public class PBKDF2WithHmacDigester extends SaltedDigester {

    private int numberOfIterations;

    private int keyLength;

    @Override
    protected byte[] generateDigest(String password, byte[] salt) {
        char[] passwordChars = password.toCharArray();

        PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, numberOfIterations, keyLength);
        try {
            SecretKeyFactory key = SecretKeyFactory.getInstance(algorithm);
            return key.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Unable to compute hash for password", e);
        }
    }

    @Override
    public void setParams(Map<String, String> params) {
        super.setParams(params);

        for (Entry<String, String> entry : params.entrySet()) {
            switch (entry.getKey()) {
            case "numberOfIterations":
                numberOfIterations = Integer.parseInt(entry.getValue());
                break;
            case "keyLength":
                keyLength = Integer.parseInt(entry.getValue());
                break;
            }
        }
    }
}
