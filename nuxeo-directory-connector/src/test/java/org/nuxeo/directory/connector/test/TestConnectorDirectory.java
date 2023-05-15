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
 *     Thierry Delprat
 */
package org.nuxeo.directory.connector.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(DirectoryFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.directory.connector:OSGI-INF/connectorbased-directory-framework.xml")
public class TestConnectorDirectory {

    @Test
    @Deploy("org.nuxeo.directory.connector.test:OSGI-INF/testContrib.xml")
    public void testContrib() throws Exception {

        DirectoryService ds = Framework.getService(DirectoryService.class);
        assertNotNull(ds);

        List<String> dsNames = ds.getDirectoryNames();

        assertTrue(dsNames.contains("testConnector"));

        Directory d = ds.getDirectory("testConnector");

        Session session = d.getSession();

        assertNotNull(session);

        DocumentModelList entries = session.getEntries();
        assertEquals(2, entries.totalSize());

        DocumentModel entry = session.getEntry("toto");
        assertNotNull(entry);
        assertEquals("Toto", (String) entry.getProperty("user", "firstName"));

        entry = session.getEntry("toti");
        assertNull(entry);

        boolean auth = session.authenticate("toto", "password");
        assertTrue(auth);
        auth = session.authenticate("toto", "whatever");
        assertFalse(auth);
        auth = session.authenticate("toti", "whatever");
        assertFalse(auth);

        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        filter.put("username", "to");
        entries = session.query(filter);
        assertNotNull(entries);
        assertEquals(1, entries.totalSize());

        filter.put("username", "t");
        entries = session.query(filter);
        assertNotNull(entries);
        assertEquals(2, entries.totalSize());

        filter.put("username", "x");
        entries = session.query(filter);
        assertNotNull(entries);
        assertEquals(0, entries.totalSize());
    }
    

    @Test
    @Deploy("org.nuxeo.directory.connector.test:OSGI-INF/testJsonDirectoryConnectorContrib.xml")
    public void testJsonDirectoryConnectorContrib() throws Exception {

        DirectoryService ds = Framework.getService(DirectoryService.class);
        assertNotNull(ds);

        List<String> dsNames = ds.getDirectoryNames();
        assertTrue(dsNames.contains("jsonDirectoryConnector"));

        Directory d = ds.getDirectory("jsonDirectoryConnector");

        Session session = d.getSession();
        assertNotNull(session);

        DocumentModelList entries = session.query(Collections.emptyMap());
        assertNotNull(entries);
        assertEquals(50, entries.totalSize());

        DocumentModel entry = session.getEntry("358317744");
        assertNotNull(entry);
        assertEquals("The Sea", (String) entry.getProperty("itunes", "trackName"));

    }

    @Test
    @Deploy("org.nuxeo.directory.connector.test:OSGI-INF/testJsonDirectoryConnectorContrib.xml")
    public void testNasaDirectoryConnectorContrib() throws Exception {

        DirectoryService ds = Framework.getService(DirectoryService.class);
        assertNotNull(ds);

        List<String> dsNames = ds.getDirectoryNames();
        assertTrue(dsNames.contains("nasaCategories"));

        Directory d = ds.getDirectory("nasaCategories");

        Session session = d.getSession();
        assertNotNull(session);

        DocumentModelList entries = session.getEntries();
        assertNotNull(entries);
        assertEquals(13, entries.totalSize());

        DocumentModel entry = session.getEntry("12");
        assertNotNull(entry);
        assertEquals("Volcanoes", (String) entry.getProperty("vocabulary", "label"));
    }

    @Test
    @Deploy("org.nuxeo.directory.connector.test:OSGI-INF/testJsonDirectoryConnectorContrib.xml")
    @Ignore("Service is down ?")
    public void testNasaDSDirectoryConnectorContrib() throws Exception {

        DirectoryService ds = Framework.getService(DirectoryService.class);
        assertNotNull(ds);

        List<String> dsNames = ds.getDirectoryNames();
        assertTrue(dsNames.contains("nasaDataSets"));

        Directory d = ds.getDirectory("nasaDataSets");

        Session session = d.getSession();
        assertNotNull(session);

        Map<String, Serializable> filter = new HashMap<>();
        filter.put("category", "322");

        DocumentModelList entries = session.query(filter);
        assertNotNull(entries);
        assertEquals(10, entries.totalSize());

        DocumentModel entry = session.getEntry("707");
        assertNotNull(entry);
        assertEquals("lunar-sample-atlas", (String) entry.getProperty("nasads", "slug"));

    }
}