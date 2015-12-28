/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *     Damien Metzler
 */
package org.nuxeo.ecm.directory.digest;

import org.nuxeo.runtime.api.Framework;

/**
 * Helper to check passwords and generated hashed salted ones.
 */
public class PasswordHelper {

    /**
     * Checks if a password is already hashed.
     *
     * @param password
     * @return {@code true} if the password is hashed
     */
    public static boolean isHashed(String password) {
        String name = getDigesterService().getDigesterNameFromHash(password);
        if (name == null) {
            return false;
        }
        try {
            getDigesterService().getPasswordDigester(name);
            return true;
        } catch (UnknownAlgorithmException e) {
            return false;
        }
    }

    private static PasswordDigesterService getDigesterService() {
        return Framework.getService(PasswordDigesterService.class);
    }

    /**
     * Returns the hashed string for a password according to a given hashing algorithm.
     *
     * @param algorithm the algorithm, {@link #SSHA} or {@link #SMD5}, or {@code null} to not hash
     * @param password the password
     * @return the hashed password
     */
    public static String hashPassword(String password, String algorithm) {
        if (algorithm == null || "".equals(algorithm)) {
            return password;
        }

        PasswordDigester digester = getDigesterService().getPasswordDigester(algorithm);
        return digester.hashPassword(password);

    }

    /**
     * Verify a password against a hashed password.
     *
     * @param password the password to verify
     * @param hashedPassword the hashed password
     * @return {@code true} if the password matches
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
        // Extract method from hashed password
        PasswordDigesterService ds = getDigesterService();
        String digesterName = ds.getDigesterNameFromHash(hashedPassword);
        if (digesterName == null) {
            return password.equals(hashedPassword);
        }

        return ds.getPasswordDigester(digesterName).verifyPassword(password, hashedPassword);
    }

}
