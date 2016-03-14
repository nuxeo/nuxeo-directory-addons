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
 *     Maxime Hilaire
 *
 */

package org.nuxeo.ecm.directory.repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.AbstractReference;
import org.nuxeo.ecm.directory.DirectoryEntryNotFoundException;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Reference;

/**
 * Class to handle reference
 *
 * @since 5.9.6
 */
@XObject(value = "repositoryDirectoryReference")
public class RepositoryDirectoryReference extends AbstractReference {

    @XNode("@field")
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    @XNode("@directory")
    public void setTargetDirectoryName(String targetDirectoryName) {
        this.targetDirectoryName = targetDirectoryName;
    }

    @XNode("@targetField")
    protected String targetField;

    private static final Log log = LogFactory.getLog(RepositoryDirectoryReference.class);

    String fieldName;

    public void addLinks(String sourceId, List<String> targetIds) throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    public void addLinks(List<String> sourceIds, String targetId) throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    protected interface Collector {
        List<String> collect(Reference dir) throws DirectoryException;
    }

    protected List<String> doCollect(Collector extractor) throws DirectoryException {
        Set<String> ids = new HashSet<String>();
        Reference ref = getSourceDirectory().getReference(fieldName);
        if (ref != null) {
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

    public List<String> getSourceIdsForTarget(final String targetId) throws DirectoryException {
        return doCollect(new Collector() {
            public List<String> collect(Reference ref) throws DirectoryException {
                return ref.getSourceIdsForTarget(targetId);
            }
        });
    }

    public List<String> getTargetIdsForSource(final String sourceId) throws DirectoryException {
        return doCollect(new Collector() {
            public List<String> collect(Reference ref) throws DirectoryException {
                return ref.getSourceIdsForTarget(sourceId);
            }
        });
    }

    public void removeLinksForSource(String sourceId) throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    public void removeLinksForTarget(String targetId) throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    public void setSourceIdsForTarget(String targetId, List<String> sourceIds) throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    public void setTargetIdsForSource(String sourceId, List<String> targetIds) throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public RepositoryDirectoryReference clone() {
        RepositoryDirectoryReference clone = (RepositoryDirectoryReference) super.clone();
        // basic fields are already copied by super.clone()
        return clone;
    }

}
