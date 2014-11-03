/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

public class ResilientReference extends AbstractReference {

    private static final Log log = LogFactory.getLog(ResilientReference.class);

    final ResilientDirectory dir;

    final String fieldName;

    ResilientReference(ResilientDirectory dir, String fieldName) {
        this.dir = dir;
        this.fieldName = fieldName;
    }

    @Override
    public void addLinks(String sourceId, List<String> targetIds)
            throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addLinks(List<String> sourceIds, String targetId)
            throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    protected interface Collector {
        List<String> collect(Reference dir) throws DirectoryException;
    }

    protected List<String> doCollect(Collector extractor)
            throws DirectoryException {
        Set<String> ids = new HashSet<String>();
        for (SubDirectoryDescriptor sub : dir.getDescriptor().subDirectories) {

            Directory dir = ResilientDirectoryFactory.getDirectoryService().getDirectory(
                    sub.name);
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
    public List<String> getSourceIdsForTarget(final String targetId)
            throws DirectoryException {
        return doCollect(new Collector() {
            @Override
            public List<String> collect(Reference ref)
                    throws DirectoryException {
                return ref.getSourceIdsForTarget(targetId);
            }
        });
    }

    @Override
    public List<String> getTargetIdsForSource(final String sourceId)
            throws DirectoryException {
        return doCollect(new Collector() {
            @Override
            public List<String> collect(Reference ref)
                    throws DirectoryException {
                return ref.getSourceIdsForTarget(sourceId);
            }
        });
    }

    @Override
    public void removeLinksForSource(String sourceId) throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeLinksForTarget(String targetId) throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSourceIdsForTarget(String targetId, List<String> sourceIds)
            throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTargetIdsForSource(String sourceId, List<String> targetIds)
            throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 5.6
     */
    @Override
    protected AbstractReference newInstance() {
        return new ResilientReference(dir, fieldName);
    }

    /**
     * @since 5.6
     */
    @Override
    public AbstractReference clone() {
        return super.clone();
    }

}
