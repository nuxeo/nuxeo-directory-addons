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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor;
import org.nuxeo.ecm.directory.InverseReference;
import org.nuxeo.ecm.directory.repository.intercept.DirectorySessionWrapper;
import org.nuxeo.ecm.directory.repository.intercept.SimpleForward;
import org.nuxeo.runtime.api.Framework;

/**
 * Directory on top of repository descriptor
 *
 * @since 5.9.6
 */
@XObject(value = "directory")
public class RepositoryDirectoryDescriptor extends BaseDirectoryDescriptor {

    protected static final Log log = LogFactory.getLog(RepositoryDirectoryDescriptor.class);

    // TODO unused
    public static final boolean DEFAULT_AUTO_VERSIONING = false;

    public static final String DEFAULT_CREATE_PATH = "/";

    public static final boolean DEFAULT_CAN_CREATE_ROOT_FOLDER = true;

    @XNode("docType")
    protected String docType;

    @XNode("querySizeLimit")
    public Integer querySizeLimit;

    // TODO unused
    @XNode("autoVersioning")
    public Boolean autoVersioning;

    // TODO unused
    public boolean getAutoVersioning() {
        return autoVersioning == null ? DEFAULT_AUTO_VERSIONING : autoVersioning.booleanValue();
    }

    @XNode("repositoryName")
    protected String repositoryName;

    @XNode("createPath")
    protected String createPath;

    public String getCreatePath() {
        return createPath == null ? DEFAULT_CREATE_PATH : createPath;
    }

    @XNode("canCreateRootFolder")
    protected Boolean canCreateRootFolder;

    public boolean canCreateRootFolder() {
        return canCreateRootFolder == null ? DEFAULT_CAN_CREATE_ROOT_FOLDER : canCreateRootFolder.booleanValue();
    }

    // TODO unused
    @XNodeList(value = "references/repositoryDirectoryReference", type = RepositoryDirectoryReference[].class, componentType = RepositoryDirectoryReference.class)
    private RepositoryDirectoryReference[] repositoryDirectoryReference;

    // TODO unused
    @XNodeList(value = "references/inverseReference", type = InverseReference[].class, componentType = InverseReference.class)
    private InverseReference[] inverseReferences;

    @XNodeMap(value = "fieldMapping", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> fieldMapping = new HashMap<String, String>();

    @XNodeList(value = "acl", type = ACLDescriptor[].class, componentType = ACLDescriptor.class)
    protected ACLDescriptor[] acls;

    @XNode("wrapperClass")
    protected Class<? extends DirectorySessionWrapper> wrapperClass;

    protected DirectorySessionWrapper wrapper;

    @Override
    public RepositoryDirectoryDescriptor clone() {
        RepositoryDirectoryDescriptor clone = (RepositoryDirectoryDescriptor) super.clone();
        // basic fields are already copied by super.clone()
        clone.fieldMapping = fieldMapping;
        if (acls != null) {
            clone.acls = acls.clone();
        }
        return clone;
    }

    public String getRepositoryName() {
        if (repositoryName == null || repositoryName.isEmpty()) {
            repositoryName = Framework.getService(RepositoryManager.class).getDefaultRepositoryName();
        }
        return repositoryName;
    }

    @Override
    public void merge(BaseDirectoryDescriptor other) {
        super.merge(other);
        if (other instanceof RepositoryDirectoryDescriptor) {
            merge((RepositoryDirectoryDescriptor) other);
        }
    }

    protected void merge(RepositoryDirectoryDescriptor other) {
        if (other.docType != null) {
            docType = other.docType;
        }
        if (other.querySizeLimit != null) {
            querySizeLimit = other.querySizeLimit;
        }
        if (other.autoVersioning != null) {
            autoVersioning = other.autoVersioning;
        }
        if (other.repositoryName != null) {
            repositoryName = other.repositoryName;
        }
        if (other.createPath != null) {
            createPath = other.createPath;
        }
        if (other.docType != null) {
            docType = other.docType;
        }
        if (other.fieldMapping != null) {
            fieldMapping = other.fieldMapping;
        }
        if (other.wrapperClass != null) {
            wrapperClass = other.wrapperClass;
        }
        if (other.canCreateRootFolder != null) {
            canCreateRootFolder = other.canCreateRootFolder;
        }
        if (other.acls != null) {
            ACLDescriptor[] otherAcls = new ACLDescriptor[acls.length + other.acls.length];
            System.arraycopy(acls, 0, otherAcls, 0, acls.length);
            System.arraycopy(other.acls, 0, otherAcls, acls.length, other.acls.length);
            acls = otherAcls;
        }
    }

    @Override
    public RepositoryDirectory newDirectory() {
        return new RepositoryDirectory(this);
    }

    public DirectorySessionWrapper getWrapper() {
        if (wrapper == null) {
            if (wrapperClass != null) {
                try {
                    wrapper = wrapperClass.newInstance();
                } catch (ReflectiveOperationException e) {
                    log.error("Unable to create Wrapper class " + wrapperClass.getCanonicalName(), e);
                }
            }
            if (wrapper == null) {
                wrapper = new SimpleForward();
            }
        }
        return wrapper;
    }
}
