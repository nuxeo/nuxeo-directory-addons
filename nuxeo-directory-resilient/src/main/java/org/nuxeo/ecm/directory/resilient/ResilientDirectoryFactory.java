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
 *     Maxime Hilaire
 *
 * $Id: MultiDirectoryFactory.java 29587 2008-01-23 21:52:30Z jcarsique $
 */

package org.nuxeo.ecm.directory.resilient;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryFactory;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * @author Florent Guillaume
 * @author Maxime Hilaire
 */
public class ResilientDirectoryFactory extends DefaultComponent implements DirectoryFactory {

    private static final String NAME = "org.nuxeo.ecm.directory.resilient.ResilientDirectoryFactory";

    protected ResilentDirectoryRegistry directories;

    @Override
    public Directory getDirectory(String name) {
        return directories.getDirectory(name);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void activate(ComponentContext context) {
        directories = new ResilentDirectoryRegistry();
    }

    @Override
    public void deactivate(ComponentContext context) {
        directories = null;
    }

    public static DirectoryService getDirectoryService() {
        return Framework.getService(DirectoryService.class);
    }

    @Override
    public void registerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        DirectoryService dirService = getDirectoryService();
        for (Object contrib : contribs) {
            ResilientDirectoryDescriptor descriptor = (ResilientDirectoryDescriptor) contrib;
            directories.addContribution(descriptor);
            String name = descriptor.name;
            if (directories.getDirectory(name) != null) {
                dirService.registerDirectory(name, this);
            } else {
                // handle case where directory is marked with "remove"
                dirService.unregisterDirectory(name, this);
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws DirectoryException {
        Object[] contribs = extension.getContributions();
        DirectoryService dirService = getDirectoryService();
        for (Object contrib : contribs) {
            ResilientDirectoryDescriptor descriptor = (ResilientDirectoryDescriptor) contrib;
            String directoryName = descriptor.name;
            dirService.unregisterDirectory(directoryName, this);
            directories.removeContribution(descriptor);
        }
    }

    @Override
    public void shutdown() throws DirectoryException {
        for (Directory directory : directories.getDirectories()) {
            directory.shutdown();
        }
    }

    @Override
    public List<Directory> getDirectories() {
        return new ArrayList<Directory>(directories.getDirectories());
    }

}
