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
 */
package org.nuxeo.ecm.directory.repository.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.nuxeo.ecm.directory.digest.PasswordHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Deploy({ "org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.api", "org.nuxeo.ecm.directory.repository" })
@Features(RuntimeFeature.class)
public class TestPasswordHelper {

    @Test
    public void testVerify() {
        assertTrue(PasswordHelper.verifyPassword("abcd", "abcd"));
        assertTrue(PasswordHelper.verifyPassword("abcd", "{SSHA}WPvqVeSt0Mr2llICYmAX9+pjtPH271eznDHvrw=="));
        assertTrue(PasswordHelper.verifyPassword("abcd", "{SMD5}/wZ7JQUARlCBq4JFHI57AfbvV7OcMe+v"));
        assertFalse(PasswordHelper.verifyPassword("1234", "abcd"));
        assertFalse(PasswordHelper.verifyPassword("1234", "{SSHA}WPvqVeSt0Mr2llICYmAX9+pjtPH271eznDHvrw=="));
        assertFalse(PasswordHelper.verifyPassword("1234", "{SMD5}/wZ7JQUARlCBq4JFHI57AfbvV7OcMe+v"));
        assertFalse(PasswordHelper.verifyPassword(" abcd", "abcd"));
        assertFalse(PasswordHelper.verifyPassword("abcd", "{SSHA}WPvqVeS"));
        assertFalse(PasswordHelper.verifyPassword("abcd", "{SSHA}/wZ7JQUAR"));
        assertFalse(PasswordHelper.verifyPassword("abcd", "{SSHA}WXYZ"));
        assertFalse(PasswordHelper.verifyPassword("abcd", "{SSHA}WPvqVeSt0Mr2llICYmAX9+pjtPH271eznDHvrwfghijkl"));
        assertFalse(PasswordHelper.verifyPassword("abcd", "{SSHA}/wZ7JQUARlC*"));

        assertTrue(PasswordHelper.verifyPassword("abcdéf",
                "{PBKDF2HmacSHA1}UdzRLXPJ8UcV6+FfyLG5PvxxV9qKcCNqZHYEW1b1Ppw0jrpN1pI38Q=="));
        assertFalse(PasswordHelper.verifyPassword("abcdéf", "{PBKDF2HmacSHA1}Bla=="));
    }

    @Test
    public void testHash() {
        String password = "abcdéf";
        String hashed;
        hashed = PasswordHelper.hashPassword(password, null);
        assertEquals(password, hashed);
        assertTrue(PasswordHelper.verifyPassword(password, hashed));
        hashed = PasswordHelper.hashPassword(password, "SSHA");
        assertTrue(hashed.startsWith("{SSHA}"));
        assertTrue(PasswordHelper.verifyPassword(password, hashed));
        hashed = PasswordHelper.hashPassword(password, "SMD5");
        assertTrue(hashed.startsWith("{SMD5}"));
        assertTrue(PasswordHelper.verifyPassword(password, hashed));

        hashed = PasswordHelper.hashPassword(password, "HmacSHA256");
        assertTrue(hashed.startsWith("{HmacSHA256}"));
        assertTrue(PasswordHelper.verifyPassword(password, hashed));

        hashed = PasswordHelper.hashPassword(password, "PBKDF2HmacSHA1");
        assertTrue(hashed.startsWith("{PBKDF2HmacSHA1}"));
        assertTrue(PasswordHelper.verifyPassword(password, hashed));

    }

    @Test
    public void testIsHashed() {
        assertTrue(PasswordHelper.isHashed("{SSHA}foo"));
        assertTrue(PasswordHelper.isHashed("{SMD5}foo"));
        assertFalse(PasswordHelper.isHashed("{foo}bar"));
        assertFalse(PasswordHelper.isHashed("foo"));
    }

}
