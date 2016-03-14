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
import org.nuxeo.ecm.directory.memory.MemoryDirectoryDescriptor;
import org.nuxeo.ecm.directory.resilient.ResilientDirectory;
import org.nuxeo.runtime.api.Framework;
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

    @Inject
    protected RuntimeHarness harness;

    private MemoryDirectory memdir3;

    private MemoryDirectory memdir4;

    protected MemoryDirectoryDescriptor desc3;

    protected MemoryDirectoryDescriptor desc4;

    @Before
    public void setUp() throws Exception {
        // First deploy schema
        harness.deployContrib(TEST_BUNDLE, "schemas-config.xml");

        Set<String> schema1Set = new HashSet<String>(Arrays.asList("uid", "foo", "bar"));

        desc3 = new MemoryDirectoryDescriptor();
        desc3.name = "dir3";
        desc3.schemaName = "schema1";
        desc3.schemaSet = schema1Set;
        desc3.idField = "uid";
        desc3.passwordField = "foo";
        directoryService.registerDirectoryDescriptor(desc3);
        memdir3 = (MemoryDirectory) directoryService.getDirectory("dir3");

        desc4 = new MemoryDirectoryDescriptor();
        desc4.name = "dir4";
        desc4.schemaName = "schema1";
        desc4.schemaSet = schema1Set;
        desc4.idField = "uid";
        desc4.passwordField = "foo";
        directoryService.registerDirectoryDescriptor(desc4);
        memdir4 = (MemoryDirectory) directoryService.getDirectory("dir4");
        memdir4.setReadOnly(true);

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
        directoryService = Framework.getService(DirectoryService.class);
        directoryService.unregisterDirectoryDescriptor(desc3);
        directoryService.unregisterDirectoryDescriptor(desc4);
    }

}
