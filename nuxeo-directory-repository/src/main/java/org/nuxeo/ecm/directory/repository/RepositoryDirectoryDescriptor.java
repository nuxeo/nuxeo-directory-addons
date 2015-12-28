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
public class RepositoryDirectoryDescriptor implements Cloneable {

    protected static final Log log = LogFactory.getLog(RepositoryDirectoryDescriptor.class);

    @XNode("@name")
    public String name;

    @XNode("schema")
    protected String schemaName;

    @XNode("docType")
    protected String docType;

    @XNode("idField")
    protected String idField;

    @XNode("passwordField")
    protected String passwordField;

    @XNode("readOnly")
    public Boolean readOnly;

    @XNode("querySizeLimit")
    public Integer querySizeLimit;

    @XNode("@remove")
    public boolean remove = false;

    @XNode("autoVersioning")
    public boolean autoVersioning = false;

    @XNode("repositoryName")
    protected String repositoryName;

    @XNode("createPath")
    protected String createPath = "/";

    @XNode("canCreateRootFolder")
    public boolean canCreateRootFolder = true;

    @XNodeList(value = "references/repositoryDirectoryReference", type = RepositoryDirectoryReference[].class, componentType = RepositoryDirectoryReference.class)
    private RepositoryDirectoryReference[] repositoryDirectoryReference;

    @XNodeList(value = "references/inverseReference", type = InverseReference[].class, componentType = InverseReference.class)
    private InverseReference[] inverseReferences;

    @XNodeMap(value = "fieldMapping", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> fieldMapping = new HashMap<String, String>();

    @XNodeList(value = "acl", type = ACLDescriptor[].class, componentType = ACLDescriptor.class)
    protected ACLDescriptor[] acls;

    protected RepositoryDirectory repositoryDirectory;

    @XNode("wrapperClass")
    protected Class<? extends DirectorySessionWrapper> wrapperClass;

    protected DirectorySessionWrapper wrapper = null;

    @Override
    public RepositoryDirectoryDescriptor clone() {
        RepositoryDirectoryDescriptor clone = new RepositoryDirectoryDescriptor();
        clone.name = name;
        clone.schemaName = schemaName;
        clone.idField = idField;
        clone.passwordField = passwordField;
        clone.readOnly = readOnly;
        clone.querySizeLimit = querySizeLimit;
        clone.remove = remove;
        clone.autoVersioning = autoVersioning;
        clone.repositoryName = repositoryName;
        clone.docType = docType;
        clone.createPath = createPath;
        clone.canCreateRootFolder = canCreateRootFolder;
        clone.fieldMapping = fieldMapping;
        clone.wrapperClass = wrapperClass;
        if (acls != null) {
            clone.acls = acls;
        }
        return clone;
    }

    public String getRepositoryName() {
        if (repositoryName == null || repositoryName.isEmpty()) {
            repositoryName = Framework.getService(RepositoryManager.class).getDefaultRepositoryName();
        }
        return repositoryName;
    }

    public void merge(RepositoryDirectoryDescriptor other) {
        merge(other, false);
    }

    public void merge(RepositoryDirectoryDescriptor other, boolean overwrite) {
        if (other.schemaName != null || overwrite) {
            schemaName = other.schemaName;
        }
        if (other.docType != null || overwrite) {
            docType = other.docType;
        }
        if (other.idField != null || overwrite) {
            idField = other.idField;
        }
        if (other.passwordField != null || overwrite) {
            passwordField = other.passwordField;
        }
        if (other.readOnly != null || overwrite) {
            readOnly = other.readOnly;
        }
        if (other.querySizeLimit != null || overwrite) {
            querySizeLimit = other.querySizeLimit;
        }
        if (other.repositoryName != null || overwrite) {
            repositoryName = other.repositoryName;
        }
        if (other.createPath != null || overwrite) {
            createPath = other.createPath;
        }
        if (other.docType != null || overwrite) {
            docType = other.docType;
        }
        if (other.fieldMapping != null || overwrite) {
            fieldMapping = other.fieldMapping;
        }
        if (other.wrapperClass != null || overwrite) {
            wrapperClass = other.wrapperClass;
        }

        autoVersioning = other.autoVersioning;
        canCreateRootFolder = other.canCreateRootFolder;

        if (other.acls != null || overwrite) {
            if (acls == null) {
                acls = other.acls;
            } else {
                ACLDescriptor[] otherAcls = new ACLDescriptor[acls.length + other.acls.length];
                System.arraycopy(acls, 0, otherAcls, 0, acls.length);
                System.arraycopy(other.acls, 0, otherAcls, acls.length, other.acls.length);
                acls = otherAcls;
            }
        }
    }

    public void init() {
        repositoryDirectory = new RepositoryDirectory(this);
    }

    public void start() {
        repositoryDirectory.start();
    }

    public void stop() {
        if (repositoryDirectory != null) {
            repositoryDirectory.shutdown();
            repositoryDirectory = null;
        }
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
