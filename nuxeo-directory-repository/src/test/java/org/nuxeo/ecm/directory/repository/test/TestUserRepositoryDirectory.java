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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.security.auth.login.LoginContext;

import org.junit.After;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;

public class TestUserRepositoryDirectory extends TestRepositoryDirectory {

    protected LoginContext lc;

    @Override
    public void setUp() throws Exception {
        lc = RepositoryDirectoryFeature.loginAs(RepositoryDirectoryFeature.USER1_NAME,
                RepositoryDirectoryFeature.USER1_NAME);
        dirSession = repoDir.getSession();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        dirSession.close();
        lc.logout();
    }

    @Test
    public void testGetEntry() throws Exception {
        DocumentModel entry;
        entry = dirSession.getEntry(RepositoryDirectoryInit.DOC_ID_USER1);
        assertEquals("foo1", entry.getPropertyValue(FOO_FIELD));
        entry = dirSession.getEntry("no-such-entry");
        assertNull(entry);
        entry = dirSession.getEntry(RepositoryDirectoryInit.DOC_ID_USER2);
        assertNotNull(entry);
    }

}
