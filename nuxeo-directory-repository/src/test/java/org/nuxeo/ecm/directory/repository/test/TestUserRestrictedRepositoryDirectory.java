/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Maxime Hilaire
 *
 */
package org.nuxeo.ecm.directory.repository.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.security.auth.login.LoginContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@RunWith(FeaturesRunner.class)
@Features(RepositoryDirectoryFeature.class)
public class TestUserRestrictedRepositoryDirectory {

    @Inject
    @Named(value = RepositoryDirectoryFeature.REPO_DIRECTORY_NAME)
    protected Directory repoDir;

    protected Session dirRestrictedSession = null;

    protected LoginContext lc;
    
    @Before
    public void setUp() throws Exception {
        lc = RepositoryDirectoryFeature.loginAs(
                RepositoryDirectoryFeature.USER2_NAME,
                RepositoryDirectoryFeature.USER2_NAME);
        dirRestrictedSession = repoDir.getSession();
    }

    @After
    public void tearDown() throws Exception {
        dirRestrictedSession.close();
        lc.logout();
    }

    @Test
    public void testGetEntry() throws Exception {
        DocumentModel entry;
        entry = dirRestrictedSession.getEntry(RepositoryDirectoryInit.DOC_ID_USER2);
        assertEquals("foo2",
                entry.getPropertyValue(TestRepositoryDirectory.FOO_FIELD));
        entry = dirRestrictedSession.getEntry("no-such-entry");
        assertNull(entry);
        entry = dirRestrictedSession.getEntry(RepositoryDirectoryInit.DOC_ID_USER1);
        assertNull(entry);

    }

}
