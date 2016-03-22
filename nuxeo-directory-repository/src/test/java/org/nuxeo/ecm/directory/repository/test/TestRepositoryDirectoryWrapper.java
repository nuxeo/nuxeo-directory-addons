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
 *     Maxime Hilaire
 *
 */
package org.nuxeo.ecm.directory.repository.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.LoginService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(RepositoryDirectoryFeature.class)
public class TestRepositoryDirectoryWrapper {

    @Inject
    protected LoginService loginService;

    @Inject
    protected RuntimeHarness harness;

    protected final static String SCHEMA_NAME = "schema1";

    protected final static String USER_SCHEMA_NAME = "user";

    protected final static String PREFIX_SCHEMA = "sch1";

    protected final static String USERNAME_FIELD = "username";

    protected final static String PASSWORD_FIELD = "password";

    protected final static String COMPANY_FIELD = "company";

    protected final static String UID_FIELD = PREFIX_SCHEMA + ":" + "uid";

    protected final static String BAR_FIELD = PREFIX_SCHEMA + ":" + "bar";

    protected final static String FOO_FIELD = PREFIX_SCHEMA + ":" + "foo";

    protected Session dirSession = null;

    @Before
    public void setUp() throws Exception {
        // be sure we don't retrieve a leaked security context
        Framework.login();

        Directory repoDir = Framework.getService(DirectoryService.class).getDirectory("wrappedUserRepositoryDirectory");
        Assert.assertNotNull(repoDir);

        dirSession = repoDir.getSession();

    }

    @After
    public void tearDown() throws Exception {
        dirSession.close();
    }

    @Test
    public void testGetEntries() throws Exception {

        Assert.assertNotNull(dirSession.getEntry(RepositoryDirectoryInit.DOC_ID_USER1));
        Assert.assertNull(dirSession.getEntry("bad-id"));
        Assert.assertNotNull(dirSession.getEntry("dummy"));
    }

}
