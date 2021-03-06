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
package org.nuxeo.ecm.directory.repository.test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.directory.digest.PasswordDigester;
import org.nuxeo.ecm.directory.digest.PasswordDigesterService;
import org.nuxeo.ecm.directory.digest.UnknownAlgorithmException;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Inject;

/**
 * @since 7.1
 */
@RunWith(FeaturesRunner.class)
@Deploy({ "org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.api", "org.nuxeo.ecm.directory.repository" })
@Features(RuntimeFeature.class)
@LocalDeploy("org.nuxeo.ecm.directory.repository:remove-digester-contrib.xml")
public class PasswordDigesterServiceTest {

    @Inject
    PasswordDigesterService ds;

    @Test
    public void passwordDigesterServiceIsRegsitered() throws Exception {
        assertNotNull(ds);
    }

    @Test
    public void itGetTheSSHADigester() throws Exception {
        PasswordDigester digester = ds.getPasswordDigester("SSHA");
        assertNotNull(digester);
        assertTrue(digester.hashPassword("abcd").startsWith("{SSHA}"));
    }

    @Test(expected = UnknownAlgorithmException.class)
    public void itThrowsAnExceptionForAnUnknownAlgorithm() throws Exception {
        ds.getPasswordDigester("unknownAlgorithm");
    }

    @Test(expected = UnknownAlgorithmException.class)
    public void itIsPossibleToDisableAnAlgorithm() throws Exception {
        ds.getPasswordDigester("PBKDF2Hmac256");
    }

    @Test
    public void digesterMayVerifyAPassword() throws Exception {
        PasswordDigester digester = ds.getPasswordDigester("SSHA");
        assertTrue(digester.verifyPassword("abcd", "{SSHA}WPvqVeSt0Mr2llICYmAX9+pjtPH271eznDHvrw=="));
    }

}
