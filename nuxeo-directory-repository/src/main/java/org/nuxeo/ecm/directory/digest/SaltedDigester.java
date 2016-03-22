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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @since 7.1
 */
public class SaltedDigester extends AbstractSaltedDigester {

    protected String algorithm;

    protected static final Log log = LogFactory.getLog(SaltedDigester.class);

    @Override
    protected byte[] generateDigest(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(password.getBytes("UTF-8"));
            md.update(salt);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new UnknownAlgorithmException();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setParams(Map<String, String> params) {
        super.setParams(params);
        for (Entry<String, String> entry : params.entrySet()) {
            switch (entry.getKey()) {
            case "algorithm":
                algorithm = entry.getValue();
                break;
            }
        }
    }

}
