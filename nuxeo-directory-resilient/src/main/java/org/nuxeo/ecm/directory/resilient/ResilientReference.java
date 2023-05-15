/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.resilient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.directory.AbstractReference;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryEntryNotFoundException;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.sql.SQLSession;
import org.nuxeo.runtime.api.Framework;

public class ResilientReference extends AbstractReference {

    private static final Log log = LogFactory.getLog(ResilientReference.class);

    final ResilientDirectory dir;

    final String fieldName;

    ResilientReference(ResilientDirectory dir, String fieldName) {
        super(fieldName, dir.getName());
        this.dir = dir;
        this.fieldName = fieldName;
    }

    @Override
    public void addLinks(String sourceId, List<String> targetIds) throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addLinks(String sourceId, List<String> targetIds, Session session) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addLinks(List<String> sourceIds, String targetId) throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addLinks(List<String> sourceIds, String targetId, Session session) {
        throw new UnsupportedOperationException();
    }

    protected interface Collector {
        List<String> collect(Reference dir) throws DirectoryException;
    }

    protected List<String> doCollect(Collector extractor) throws DirectoryException {
        Set<String> ids = new HashSet<String>();
        for (SubDirectoryDescriptor sub : dir.getDescriptor().subDirectories) {

            Directory dir = Framework.getService(DirectoryService.class).getDirectory(sub.name);
            if (dir == null) {
                continue;
            }
            Reference ref = dir.getReference(fieldName);
            if (ref == null) {
                continue;
            }
            try {
                ids.addAll(extractor.collect(ref));
            } catch (DirectoryEntryNotFoundException e) {
                log.debug(e.getMessage());
            }
        }
        List<String> x = new ArrayList<String>(ids.size());
        x.addAll(ids);
        return x;
    }

    @Override
    public List<String> getSourceIdsForTarget(final String targetId) throws DirectoryException {
        return doCollect(new Collector() {
            @Override
            public List<String> collect(Reference ref) throws DirectoryException {
                return ref.getSourceIdsForTarget(targetId);
            }
        });
    }

    @Override
    public List<String> getTargetIdsForSource(final String sourceId) throws DirectoryException {
        return doCollect(new Collector() {
            @Override
            public List<String> collect(Reference ref) throws DirectoryException {
                return ref.getSourceIdsForTarget(sourceId);
            }
        });
    }

    @Override
    public void removeLinksForSource(String sourceId) throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeLinksForSource(String sourceId, Session session) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeLinksForTarget(String targetId) throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeLinksForTarget(String targetId, Session session) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSourceIdsForTarget(String targetId, List<String> sourceIds) throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSourceIdsForTarget(String targetId, List<String> sourceIds, Session session) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTargetIdsForSource(String sourceId, List<String> targetIds) throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTargetIdsForSource(String sourceId, List<String> targetIds, Session session) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws CloneNotSupportedException 
     * @since 5.6
     */
    @Override
    public ResilientReference clone() throws CloneNotSupportedException {
        ResilientReference clone = (ResilientReference) super.clone();
        // basic fields are already copied by super.clone()
        return clone;
    }

}
