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
        lc = RepositoryDirectoryFeature.loginAs(
                RepositoryDirectoryFeature.USER1_NAME,
                RepositoryDirectoryFeature.USER1_NAME);
        dirSession = repoDir.getSession();
    }
    
    
   

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
