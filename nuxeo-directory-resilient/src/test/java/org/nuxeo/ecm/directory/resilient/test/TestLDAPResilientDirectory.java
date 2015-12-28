/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id: TestMultiDirectory.java 30378 2008-02-20 17:37:26Z gracinet $
 */

package org.nuxeo.ecm.directory.resilient.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.ldap.LDAPDirectory;
import org.nuxeo.ecm.directory.resilient.ResilientDirectory;
import org.nuxeo.ecm.directory.resilient.ResilientDirectorySession;
import org.nuxeo.ecm.directory.sql.SQLDirectory;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @author Florent Guillaume
 * @author Maxime Hilaire
 */
// @Ignore("Setup issue : LdapDirectory is not deployed")
@LocalDeploy("org.nuxeo.ecm.directory.resilient.tests:resilient-ldap-sql-directories-config.xml")
public class TestLDAPResilientDirectory extends LDAPDirectoryTestCase {

    ResilientDirectory resilientUserDir;

    LDAPDirectory ldapUserDir;

    Session ldapUserSession;

    SQLDirectory sqlUserDir;

    Session sqlUserSession;

    ResilientDirectorySession resUserDirSession;

    private Session sqlGroupSession;

    private SQLDirectory sqlGroupDir;

    private Session ldapGroupSession;

    private LDAPDirectory ldapGroupDir;

    private ResilientDirectorySession resGroupDirSession;

    private ResilientDirectory resilientGroupDir;

    @Before
    public void setUpResilient() throws Exception {
        // the USER resilient directory
        resilientUserDir = (ResilientDirectory) directoryService.getDirectory("resilientUserDirectory");
        resUserDirSession = (ResilientDirectorySession) resilientUserDir.getSession();

        ldapUserDir = getLDAPDirectory("ldapUserDirectory");
        ldapUserSession = ldapUserDir.getSession();

        sqlUserDir = (SQLDirectory) (directoryService.getDirectory("sqlUserDirectory"));
        sqlUserSession = sqlUserDir.getSession();

        // the GROUP resilient directory
        resilientGroupDir = (ResilientDirectory) directoryService.getDirectory("resilientGroupDirectory");
        resGroupDirSession = (ResilientDirectorySession) resilientGroupDir.getSession();

        ldapGroupDir = getLDAPDirectory("ldapGroupDirectory");
        ldapGroupSession = ldapGroupDir.getSession();

        sqlGroupDir = (SQLDirectory) (directoryService.getDirectory("sqlGroupDirectory"));
        sqlGroupSession = sqlGroupDir.getSession();

    }

    @Test
    public void testCreateEntry() {
        if (USE_EXTERNAL_TEST_LDAP_SERVER) {
            HashMap<String, Object> e = new HashMap<String, Object>();
            e.put("usr:username", "myUser");
            e.put("usr:password", "secret");
            DocumentModel doc = resUserDirSession.createEntry(e);
            assertNotNull(doc);
            doc = sqlUserSession.getEntry("myUser");
            assertNotNull(doc);
        }
    }

    @Ignore("NXP-17461")
    @Test
    public void testFallbackOnGetEntry() throws Exception {
        DocumentModel entry = resUserDirSession.getEntry("user1");
        assertNotNull(entry);

        Map<String, Object> propsLDAP = ldapUserSession.getEntry("user1").getProperties("user");
        shutdownLdapServer();
        Map<String, Object> propsSQL = resUserDirSession.getEntry("user1").getProperties("user");
        assertEquals(propsLDAP, propsSQL);

    }

    @Test
    public void testGetUserGroup() throws Exception {
        if (USE_EXTERNAL_TEST_LDAP_SERVER) {
            DocumentModel ldapUser = resUserDirSession.getEntry("user1");
            assertNotNull(ldapUser);
            List<String> ldapUserGroups = (List<String>) ldapUser.getProperty("user", "groups");
            assertNotNull(ldapUserGroups);

            DocumentModel sqlUser = sqlUserSession.getEntry("user1");
            assertNotNull(sqlUser);
            List<String> sqlUserGroups = (List<String>) sqlUser.getProperty("user", "groups");
            assertEquals(ldapUserGroups, sqlUserGroups);

            DocumentModel ldapGroup = ldapGroupSession.getEntry(sqlUserGroups.get(0));
            resGroupDirSession.getEntry(ldapGroup.getId());
            DocumentModel sqlGroup = sqlGroupSession.getEntry(sqlUserGroups.get(0));
            assertEquals(ldapGroup, sqlGroup);
        }

    }

    @Test
    public void testUpdateUser() {
        if (USE_EXTERNAL_TEST_LDAP_SERVER) {
            DocumentModel ldapUser = resUserDirSession.getEntry("user1");
            assertNotNull(ldapUser);
            List<String> ldapUserGroups = (List<String>) ldapUser.getProperty("user", "groups");
            assertNotNull(ldapUserGroups);

            ldapUser.setProperty("user", "firstName", "user1-updated");
            resUserDirSession.updateEntry(ldapUser);

            DocumentModel sqlUser = sqlUserSession.getEntry("user1");
            assertEquals(ldapUser, sqlUser);

        }
    }

    @Test
    public void testAuthenticate() throws Exception {
        // Not possible to authenticate against internal ldap server
        if (USE_EXTERNAL_TEST_LDAP_SERVER) {
            assertTrue(resUserDirSession.authenticate("user1", "secret"));
            // Shutdown manually your external LDAP to test fallback
            assertTrue(resUserDirSession.authenticate("user1", "secret"));
        }

    }

    // Only for LDAP fallback test purpose
    protected void shutdownLdapServer() {
        if (!USE_EXTERNAL_TEST_LDAP_SERVER) {
            server.shutdownLdapServer();
        }
    }

}
