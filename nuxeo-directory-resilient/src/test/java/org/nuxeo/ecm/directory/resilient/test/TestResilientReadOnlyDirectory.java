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

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.memory.MemoryDirectory;
import org.nuxeo.ecm.directory.memory.MemoryDirectoryFactory;
import org.nuxeo.ecm.directory.resilient.ResilientDirectory;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

/**
 * @author Florent Guillaume
 * @author Maxime Hilaire
 */

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class)
@Deploy({ "org.nuxeo.ecm.directory.api", "org.nuxeo.ecm.directory", "org.nuxeo.ecm.core.schema",
        "org.nuxeo.ecm.directory.types.contrib", "org.nuxeo.ecm.directory.resilient" })
public class TestResilientReadOnlyDirectory {

    private static final String TEST_BUNDLE = "org.nuxeo.ecm.directory.resilient.tests";

    @Inject
    DirectoryService directoryService;

    MemoryDirectoryFactory memoryDirectoryFactory;

    @Inject
    protected RuntimeHarness harness;

    private MemoryDirectory memdir3;

    private MemoryDirectory memdir4;

    @Before
    public void setUp() throws Exception {
        // First deploy schema
        harness.deployContrib(TEST_BUNDLE, "schemas-config.xml");

        // Define memory directory
        memoryDirectoryFactory = new MemoryDirectoryFactory();
        directoryService.registerDirectory("memdirs", memoryDirectoryFactory);

        Set<String> schema1Set = new HashSet<String>(Arrays.asList("uid", "foo", "bar"));

        memdir3 = new MemoryDirectory("dir3", "schema1", schema1Set, "uid", "foo");
        memoryDirectoryFactory.registerDirectory(memdir3);

        memdir4 = new MemoryDirectory("dir4", "schema1", schema1Set, "uid", "foo");
        memdir4.setReadOnly(true);
        memoryDirectoryFactory.registerDirectory(memdir4);

        // Deploy resilient contrib
        harness.deployContrib(TEST_BUNDLE, "resilient-memory-read-only-directories-config.xml");

    }

    @Test
    public void testConfigSlaveReadOnly() throws DirectoryException {

        try {
            ResilientDirectory resilientDir = (ResilientDirectory) directoryService.getDirectory("readOnlyResilient");
            resilientDir.getSession();
            fail("Should raise read-only exception on slave");
        } catch (Exception ex) {
        }

    }

    @After
    public void tearDown() {
        memoryDirectoryFactory.unregisterDirectory(memdir3);
        memoryDirectoryFactory.unregisterDirectory(memdir4);
    }

}
