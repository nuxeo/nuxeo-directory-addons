/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 *
 * $Id: TestMultiDirectory.java 30378 2008-02-20 17:37:26Z gracinet $
 */

package org.nuxeo.ecm.directory.resilient.test;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Florent Guillaume
 * @author Maxime Hilaire
 *
 */
public class TestUserManagerOnResilientDirectory extends LDAPDirectoryTestCase {

    private static final String TEST_BUNDLE = "org.nuxeo.ecm.directory.resilient.tests";

    DirectoryService directoryService;

    private UserManager userManager;

    private String username1 = "user1";
    private String password1 = "secret";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.platform.usermanager");
        deployBundle("org.nuxeo.ecm.platform.usermanager.api");

        // Bundle to be tested
        deployBundle("org.nuxeo.ecm.directory.resilient");


        // Config for the tested bundle
        deployContrib(TEST_BUNDLE, "resilient-ldap-sql-directories-config.xml");
        deployContrib(TEST_BUNDLE, "test-contrib-usermanager-config.xml");

        // mem dir factory
        directoryService = Framework.getLocalService(DirectoryService.class);



        userManager = Framework.getLocalService(UserManager.class);



    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testCreateUpdateUser()
    {
        if(USE_EXTERNAL_TEST_LDAP_SERVER)
        {
            String testUsername = "John";
            String testGroup = "johnsGroup";

            DocumentModel userModel = userManager.getBareUserModel();
            userModel.setProperty("user", "username", testUsername);
            userModel = userManager.createUser(userModel);

            DocumentModel groupModel = userManager.getBareGroupModel();
            groupModel.setProperty("group", "groupname", testGroup);
            groupModel = userManager.createGroup(groupModel);

            // Check entry parameters
            Assert.assertNotNull("Expected the user to exist",
                    userManager.getPrincipal(testUsername));
            Assert.assertNotNull("Expected the group to exist",
                    userManager.getGroup(testGroup));

            // Add user to group
            AddUserToGroup(testUsername, testGroup);

            // Get result
            NuxeoPrincipal user = userManager.getPrincipal(testUsername);
            boolean result = user.isMemberOf(testGroup);

            // Check result
            Assert.assertEquals("Expected the user to be added to the group", true,
                    result);
            
            String testFirstName = testUsername + "-updated";
            
            userModel.setProperty("user", "firstName", testFirstName);
            userManager.updateUser(userModel);
            
            
            DocumentModel newGroupModel = userManager.getBareGroupModel();
            newGroupModel.setProperty("group", "groupname", testGroup+"2");
            userManager.createGroup(newGroupModel);
            
            // Add user to group
            AddUserToGroup(testUsername, testGroup+"2");

            // Get result
            user = userManager.getPrincipal(testUsername);
            result = user.isMemberOf(testGroup);
            result = user.isMemberOf(testGroup+"2");

            // Check result
            Assert.assertEquals("Expected the user to be added to the new group and the updated group", true,
                    result);
            
        }
    }
    
    @Test
    public void TestCreateUserAndGroup() throws ClientException
    {
        if(USE_EXTERNAL_TEST_LDAP_SERVER)
        {
            String testUsername = "John";
            String testGroup = "johnsGroup";

            DocumentModel userModel = userManager.getBareUserModel();
            userModel.setProperty("user", "username", testUsername);
            userManager.createUser(userModel);

            DocumentModel groupModel = userManager.getBareGroupModel();
            groupModel.setProperty("group", "groupname", testGroup);
            userManager.createGroup(groupModel);

            // Check entry parameters
            Assert.assertNotNull("Expected the user to exist",
                    userManager.getPrincipal(testUsername));
            Assert.assertNotNull("Expected the group to exist",
                    userManager.getGroup(testGroup));

            // Add user to group
            AddUserToGroup(testUsername, testGroup);

            // Get result
            NuxeoPrincipal user = userManager.getPrincipal(testUsername);
            boolean result = user.isMemberOf(testGroup);

            // Check result
            Assert.assertEquals("Expected the user to be added to the group", true,
                    result);
        }
    }

    private void AddUserToGroup(String username, String group) throws ClientException
    {
        // Get user
        NuxeoPrincipal user = userManager.getPrincipal(username);

        // Add user to the group if necessary
        if (user.isMemberOf(group)) {
            return;
        }
        String userSchemaName = "user";
        String groupsFieldName = "groups";

        DocumentModel userDoc = userManager.getUserModel(username);
        List<String> userGroups = (List<String>) userDoc.getProperty(
                userSchemaName, groupsFieldName);
        userGroups.add(group);
        userDoc.setProperty(userSchemaName, groupsFieldName, userGroups);
        userManager.updateUser(userDoc);
    }

    @Test
    public void TestAuthenticate() throws Exception
    {
        if(USE_EXTERNAL_TEST_LDAP_SERVER)
        {
            Assert.assertNotNull(userManager.authenticate(username1, password1));
            //Shutdown manually your LDAP server to test fallback against authenticate
            Assert.assertNotNull(userManager.authenticate(username1, password1));
        }
    }

    @Test
    public void TestFallback() throws Exception
    {
        //Get
        NuxeoPrincipal userPrincip = userManager.getPrincipal(username1);
        shutdownLdapServer();
        NuxeoPrincipal userPrincipFall = userManager.getPrincipal(username1);
        Assert.assertEquals(userPrincip, userPrincipFall);

    }




    // Only for LDAP fallback test purpose
    protected void shutdownLdapServer() {
        if (!USE_EXTERNAL_TEST_LDAP_SERVER) {
            server.shutdownLdapServer();
        }
    }

}
