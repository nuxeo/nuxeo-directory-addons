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

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * @since 7.1
 */
public class HmacDigester extends SaltedDigester {

    @Override
    protected byte[] generateDigest(String password, byte[] salt) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(salt, algorithm);
            Mac mac = Mac.getInstance(algorithm);
            mac.init(signingKey);

            // compute the hmac on input data bytes
            return mac.doFinal(password.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new UnknownAlgorithmException(e);
        } catch (InvalidKeyException | IllegalStateException | UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to compute hash for password", e);
        }
    }
}
