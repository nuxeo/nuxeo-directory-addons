/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     mhilaire
 *
 * $Id: TestMultiDirectory.java 30378 2008-02-20 17:37:26Z gracinet $
 */

package org.nuxeo.ecm.directory.resilient.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelComparator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.memory.MemoryDirectory;
import org.nuxeo.ecm.directory.memory.MemoryDirectoryFactory;
import org.nuxeo.ecm.directory.resilient.ResilientDirectory;
import org.nuxeo.ecm.directory.resilient.ResilientDirectorySession;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

/**
 * @author Florent Guillaume
 * @author Maxime Hilaire
 *
 */

@RunWith(FeaturesRunner.class)
@Features({TransactionalFeature.class , CoreFeature.class})
@RepositoryConfig(init = DefaultRepositoryInit.class)
@Deploy({ "org.nuxeo.ecm.directory.api", "org.nuxeo.ecm.directory",
        "org.nuxeo.ecm.core.schema", "org.nuxeo.ecm.directory.types.contrib",
        "org.nuxeo.ecm.directory.resilient" })
public class TestResilientDirectory {

    private static final String TEST_BUNDLE = "org.nuxeo.ecm.directory.resilient.tests";

    @Inject
    DirectoryService directoryService;

    MemoryDirectoryFactory memoryDirectoryFactory;

    MemoryDirectory memdir1;

    MemoryDirectory memdir2;

    ResilientDirectory resilientDir;

    ResilientDirectorySession dir;

    @Inject
    protected RuntimeHarness harness;

    @Before
    public void setUp() throws Exception {

        // Deploy custom schema
        harness.deployContrib(TEST_BUNDLE, "schemas-config.xml");

        // mem dir factory
        memoryDirectoryFactory = new MemoryDirectoryFactory();
        directoryService.registerDirectory("memdirs", memoryDirectoryFactory);

        // create and register mem directories
        Map<String, Object> e;

        // Define the schema used for queries
        Set<String> schema1Set = new HashSet<String>(Arrays.asList("uid",
                "foo", "bar"));

        // dir 1
        // Define here the in-memory directory as :
        //
        // <directory name="dir1">
        // <schema>schema1</schema>
        // <idField>uid</idField>
        // <passwordField>foo</passwordField>
        // </directory>

        memdir1 = new MemoryDirectory("dir1", "schema1", schema1Set, "uid",
                "foo");
        memoryDirectoryFactory.registerDirectory(memdir1);

        Session dir1 = memdir1.getSession();
        e = new HashMap<String, Object>();
        e.put("uid", "1");
        e.put("foo", "foo1");
        e.put("bar", "bar1");
        dir1.createEntry(e);

        e = new HashMap<String, Object>();
        e.put("uid", "4");
        e.put("foo", "foo4");
        e.put("bar", "bar4");
        dir1.createEntry(e);

        // dir 2
        memdir2 = new MemoryDirectory("dir2", "schema1", schema1Set, "uid",
                "foo");
        memoryDirectoryFactory.registerDirectory(memdir2);

        Session dir2 = memdir2.getSession();
        e = new HashMap<String, Object>();
        e.put("uid", "2");
        e.put("foo", "foo2");
        e.put("bar", "bar2");
        dir2.createEntry(e);

        // Bundle to be tested
        // deployBundle("org.nuxeo.ecm.directory.resilient");

        // Config for the tested bundle
        harness.deployContrib(TEST_BUNDLE,
                "resilient-memory-directories-config.xml");

        // the resilient directory
        resilientDir = (ResilientDirectory) directoryService.getDirectory("resilient");
        dir = (ResilientDirectorySession) resilientDir.getSession();
    }

    @After
    public void tearDown() throws Exception {
        memoryDirectoryFactory.unregisterDirectory(memdir1);
        memoryDirectoryFactory.unregisterDirectory(memdir2);
        directoryService.unregisterDirectory("memdirs", memoryDirectoryFactory);
        // super.tearDown();
    }

    @Test
    public void testCreateEntry() throws Exception {
        Session dir1 = memdir1.getSession();
        Session dir2 = memdir2.getSession();

        Map<String, Object> e;

        assertEquals("foo2", dir2.getEntry("2").getProperty("schema1", "foo"));

        e = new HashMap<String, Object>();
        e.put("uid", "2");
        e.put("foo", "foo3");
        e.put("bar", "bar3");
        DocumentModel doc = dir.createEntry(e);

        assertFalse(dir1.getEntry("2") == null);
        assertFalse(dir2.getEntry("2") == null);
        assertEquals("bar3", doc.getProperty("schema1", "bar"));
        assertEquals("foo3", dir1.getEntry("2").getProperty("schema1", "foo"));
        assertEquals("foo3", dir2.getEntry("2").getProperty("schema1", "foo"));
    }

    @Test
    public void testUpdateEntry() throws Exception {
        Session dir1 = memdir1.getSession();
        Session dir2 = memdir2.getSession();

        Map<String, Object> e;

        assertEquals("foo1", dir1.getEntry("1").getProperty("schema1", "foo"));
        assertNull(dir2.getEntry("1"));

        e = new HashMap<String, Object>();
        e.put("uid", "1");
        e.put("foo", "foo3");
        e.put("bar", "bar3");

        DocumentModel docModel = dir1.getEntry("1");
        docModel.setProperties("schema1", e);

        dir.updateEntry(docModel);

        assertEquals("foo3", dir1.getEntry("1").getProperty("schema1", "foo"));
        assertEquals(docModel, dir2.getEntry("1"));
        assertEquals("foo3", dir2.getEntry("1").getProperty("schema1", "foo"));
    }

    @Test
    public void testGetEntry() throws Exception {
        DocumentModel entry;
        entry = dir.getEntry("1");
        assertEquals("1", entry.getProperty("schema1", "uid"));
        assertEquals("foo1", entry.getProperty("schema1", "foo"));
        entry = dir.getEntry("no-such-entry");
        assertNull(entry);
    }

    @Test
    public void testReplicateOnGetEntry() throws Exception {
        DocumentModel entry;

        Session dir2 = memdir2.getSession();
        entry = dir2.getEntry("1");
        assertNull(entry);

        entry = dir.getEntry("1");
        assertEquals("1", entry.getProperty("schema1", "uid"));

        entry = dir2.getEntry("1");
        assertNotNull(entry);
    }

    @Test
    public void testDeleteOnGetEntry() throws Exception {
        DocumentModel entry;

        Session dir2 = memdir2.getSession();
        entry = dir2.getEntry("2");
        assertNotNull(entry);

        entry = dir.getEntry("2");
        assertNull(entry);

        entry = dir2.getEntry("2");
        assertNull(entry);
    }

    @Test
    public void testAuthenticate() throws Exception {
        // sub dirs
        Session dir2 = memdir2.getSession();
        assertTrue(dir.authenticate("1", "foo1"));

        assertTrue(dir2.authenticate("1", "foo1"));
        assertFalse(dir.authenticate("1", "haha"));
        assertFalse(dir.authenticate("2", "foo2"));

    }

    @Test
    public void testDeleteEntry() throws Exception {
        Session dir1 = memdir1.getSession();
        Session dir2 = memdir2.getSession();
        dir.deleteEntry("no-such-entry");
        dir.deleteEntry("2");
        dir.deleteEntry("1");
        assertNull(dir.getEntry("1"));
        assertNull(dir1.getEntry("1"));
        assertNull(dir2.getEntry("1"));
        assertNull(dir2.getEntry("2"));
    }

    @Test
    public void testHasEntry() throws Exception {
        assertTrue(dir.hasEntry("1"));
        assertFalse(dir.hasEntry("foo"));
    }

    @Test
    public void testQuery() throws Exception {
        Session dir2 = memdir2.getSession();

        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        DocumentModelList entries;
        DocumentModel e;

        // empty filter means everything (like getEntries)
        entries = dir.query(filter);
        assertNotNull(entries);
        assertEquals(2, entries.size());

        // no result
        filter.put("foo", "f");
        entries = dir.query(filter);
        assertEquals(0, entries.size());

        // query to test :
        // the result is in the master and should be replicated in slave
        filter.put("foo", "foo1");
        entries = dir.query(filter);
        assertEquals(1, entries.size());
        e = entries.get(0);
        assertEquals("1", e.getId());
        assertEquals("bar1", e.getProperty("schema1", "bar"));
        assertEquals(dir2.getEntry("1"), e);
        filter.clear();

        // query to test :
        // the result is only in the slave and should be removed
        filter.put("foo", "foo2");
        entries = dir.query(filter);
        assertEquals(0, entries.size());
        assertNull(dir2.getEntry("2"));
        filter.clear();
    }

    @Test
    public void testQueryFulltext() throws Exception {
        Session dir1 = memdir1.getSession();

        // Update the uid 1 on dir1 only
        HashMap<String, Object> e = new HashMap<String, Object>();
        e.put("uid", "1");
        e.put("foo", "foo3");
        e.put("bar", "bar3");
        DocumentModel docModel = dir1.getEntry("1");
        docModel.setProperties("schema1", e);
        dir1.updateEntry(docModel);

        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        Set<String> fulltext = new HashSet<String>();
        DocumentModelList entries;
        entries = dir.query(filter, fulltext);
        assertEquals(2, entries.size());
        // null fulltext set should be equivalent to empty one
        entries = dir.query(filter, null);
        assertEquals(2, entries.size());

        Session dir2 = memdir2.getSession();
        // uid2 should have been removed from slave
        DocumentModel entry = dir2.getEntry("2");
        assertNull(entry);
        // uid1 should have been copied to slave
        // And should have been updated
        assertNotNull(dir2.getEntry("1"));
        assertEquals("bar3", dir2.getEntry("1").getProperty("schema1", "bar"));
        // uid4 should have been copied to slave
        assertNotNull(dir2.getEntry("4"));
    }

    @Test
    public void testGetProjection() throws Exception {
        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        List<String> list;

        // empty filter means everything (like getEntries)
        list = dir.getProjection(filter, "uid");
        Collections.sort(list);
        assertEquals(Arrays.asList("1", "4"), list);
        list = dir.getProjection(filter, "foo");
        Collections.sort(list);
        assertEquals(Arrays.asList("foo1", "foo4"), list);
        list = dir.getProjection(filter, "bar");
        Collections.sort(list);
        assertEquals(Arrays.asList("bar1", "bar4"), list);

        // XXX test projection on unknown column
        // Is it normal that all results are returned
        filter.put("thefoobar", "foo");
        list = dir.getProjection(filter, "uid");
        assertEquals(2, list.size());

        // no result
        filter.put("foo", "f");
        list = dir.getProjection(filter, "uid");
        assertEquals(0, list.size());
        list = dir.getProjection(filter, "foo");
        assertEquals(0, list.size());
        list = dir.getProjection(filter, "bar");
        assertEquals(0, list.size());

    }

    @Test
    public void testCreateFromModel() throws Exception {
        String schema = "schema1";
        DocumentModel entry = BaseSession.createEntryModel(null, schema, null,
                null);
        entry.setProperty("schema1", "uid", "yo");

        assertNull(dir.getEntry("yo"));
        dir.createEntry(entry);
        assertNotNull(dir.getEntry("yo"));

        // create one with existing same id, must fail
        entry.setProperty("schema1", "uid", "1");
        try {
            entry = dir.createEntry(entry);
            fail("Should raise an error, entry already exists");
        } catch (DirectoryException e) {
        }
    }

    @Test
    public void testReadOnlyOnGetEntry() throws Exception {
        Session dir1 = memdir1.getSession();
        Session dir2 = memdir2.getSession();
        // by default no backing dir is readonly
        assertFalse(BaseSession.isReadOnlyEntry(dir1.getEntry("1")));
        assertFalse(BaseSession.isReadOnlyEntry(dir1.getEntry("4")));

        // Set the master memory directory to read-only
        memdir1.setReadOnly(true);

        // The resilient dir should be read-only now
        assertTrue(dir.isReadOnly());

        // all should be readonly
        assertTrue(BaseSession.isReadOnlyEntry(dir.getEntry("1")));

        assertFalse(BaseSession.isReadOnlyEntry(dir2.getEntry("1")));

        memdir2.setReadOnly(true);
        dir2 = memdir2.getSession();
        assertTrue(BaseSession.isReadOnlyEntry(dir2.getEntry("2")));

        assertTrue(BaseSession.isReadOnlyEntry(dir.getEntry("4")));

        Assert.assertNull(dir2.getEntry("4"));
    }

    @Test
    public void testReadOnlyEntryInQueryResults() throws Exception {
        Map<String, String> orderBy = new HashMap<String, String>();
        orderBy.put("schema1:uid", "asc");
        DocumentModelComparator comp = new DocumentModelComparator(orderBy);

        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        DocumentModelList results = dir.query(filter);
        Collections.sort(results, comp);

        // by default no backing dir is readonly
        assertFalse(BaseSession.isReadOnlyEntry(results.get(0)));
        assertFalse(BaseSession.isReadOnlyEntry(results.get(1)));

        memdir1.setReadOnly(true);
        results = dir.query(filter);
        Collections.sort(results, comp);
        assertTrue(BaseSession.isReadOnlyEntry(results.get(0)));
        assertTrue(BaseSession.isReadOnlyEntry(results.get(1)));

    }

}
