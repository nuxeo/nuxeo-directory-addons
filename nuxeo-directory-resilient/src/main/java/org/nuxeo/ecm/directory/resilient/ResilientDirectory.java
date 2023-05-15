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
 * $Id: MultiDirectory.java 25713 2007-10-05 16:06:58Z fguillaume $
 */

package org.nuxeo.ecm.directory.resilient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.sql.SQLDirectory;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Florent Guillaume
 * @author Maxime Hilaire
 */
public class ResilientDirectory extends AbstractDirectory {

    private String schemaName;

    private String idField;

    private String passwordField;

    private Map<String, Field> schemaFieldMap;

    public ResilientDirectory(ResilientDirectoryDescriptor descriptor) {
        super(descriptor, null);
    }

    @Override
    protected boolean doSanityChecks() {
        return false; // we compute schema, id field etc. from the sub-directories
    }

    @Override
    public ResilientDirectoryDescriptor getDescriptor() {
        return (ResilientDirectoryDescriptor) descriptor;
    }

    private boolean checkSlaveSubDirectory(String masterSchemaName) {
        ResilientDirectoryDescriptor descriptor = getDescriptor();
        boolean slaveFound = false;
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        for (SubDirectoryDescriptor sub : descriptor.subDirectories) {
            Directory subDir = directoryService.getDirectory(sub.name);
            if (subDir == null) {
                throw new DirectoryException(
                        String.format(
                                "ResilientDirectory '%s' reference an unknown sub directory '%s'! Make sure the sub directory has been previously declared and deployed.",
                                descriptor.name, sub.name));
            }
            if (!sub.isMaster()) {

                // Check each schema's slaves that match the master schema
                if (!subDir.getSchema().equalsIgnoreCase(masterSchemaName)) {
                    throw new DirectoryException(
                            String.format(
                                    "ResilientDirectory '%s' reference a slave directory '%s' that has not the same schema than the master schema %s!",
                                    descriptor.name, subDir.getName(), masterSchemaName));
                } else if (!subDir.getIdField().equalsIgnoreCase(idField)
                        && !subDir.getPasswordField().equalsIgnoreCase(passwordField)) {
                    throw new DirectoryException(
                            String.format(
                                    "ResilientDirectory '%s' reference a slave directory '%s' that has not the same idField/passwordField than the master !",
                                    descriptor.name, subDir.getName()));
                }

                else if (subDir.getSession().isReadOnly()) {
                    throw new DirectoryException(String.format(
                            "ResilientDirectory '%s' reference a slave directory '%s' that is in read-only mode !",
                            descriptor.name, subDir.getName()));
                } else if (subDir instanceof SQLDirectory) {
                    if (((SQLDirectory) subDir).getDescriptor().autoincrementIdField) {
                        throw new DirectoryException(
                                String.format(
                                        "ResilientDirectory '%s' reference a slave SQL directory '%s' that use auto-increment id! This is still not supported by the resilient directory.",
                                        descriptor.name, subDir.getName()));
                    } else {
                        slaveFound = true;
                    }
                } else {
                    slaveFound = true;
                }
            }
        }
        return slaveFound;
    }

    protected void initSubDirectories() {

        String masterSchemaName = null;
        // Find the master subdirectory and init resilient directory from the
        // master
        for (SubDirectoryDescriptor sub : getDescriptor().subDirectories) {
            Directory dir = Framework.getLocalService(DirectoryService.class).getDirectory(sub.name);
            if (sub.isMaster() && masterSchemaName == null) {
                if (dir != null) {
                    schemaName = dir.getSchema();
                    idField = dir.getIdField();
                    passwordField = dir.getPasswordField();
                    SchemaManager sm = Framework.getLocalService(SchemaManager.class);
                    Schema sch = sm.getSchema(schemaName);
                    if (sch == null) {
                        throw new DirectoryException(String.format("Unknown schema '%s' for master subdirectory '%s' ",
                                schemaName, dir.getName()));
                    }
                    final Set<String> sourceFields = new HashSet<String>();
                    for (Field f : sch.getFields()) {
                        sourceFields.add(f.getName().getLocalName());
                    }
                    if (!sourceFields.contains(idField)) {
                        throw new DirectoryException(String.format("Directory '%s' schema '%s' has no id field '%s'",
                                dir.getName(), schemaName, idField));
                    }
                    schemaFieldMap = new HashMap<String, Field>();
                    for (Field f : sch.getFields()) {
                        String fieldName = f.getName().getLocalName();
                        schemaFieldMap.put(fieldName, f);
                    }

                    masterSchemaName = schemaName;

                } else {
                    throw new DirectoryException(String.format("Unknown directory '%s' !", sub.name));
                }
            } else if (sub.isMaster() && masterSchemaName != null) {
                throw new DirectoryException(String.format("Directory '%s' subdir '%s' "
                        + "define a master source but another one exist !", descriptor.name, sub.name));
            }

        }
        // At this point we must have found a master
        if (masterSchemaName == null) {
            throw new DirectoryException(String.format("Resilient Directory '%s' has no master source !",
                    descriptor.name));
        } else {
            if (!checkSlaveSubDirectory(masterSchemaName)) {
                throw new DirectoryException(String.format("Resilient Directory '%s' has no slave source !",
                        descriptor.name));

            }
        }

    }

    @Override
    public String getName() {
        return descriptor.name;
    }

    @Override
    public String getSchema() {
        if (schemaName == null) {
            initSubDirectories();
        }
        return schemaName;
    }

    @Override
    public String getParentDirectory() {
        return null;
    }

    @Override
    public String getIdField() {
        return idField;
    }

    @Override
    public String getPasswordField() {
        return passwordField;
    }

    public Map<String, Field> getSchemaFieldMap() {
        return schemaFieldMap;
    }

    @Override
    public Session getSession() throws DirectoryException {
        if (schemaName == null) {
            initSubDirectories();
        }
        ResilientDirectorySession session = new ResilientDirectorySession(this);
        addSession(session);
        return session;
    }

    @Override
    public Reference getReference(String referenceFieldName) {
        return new ResilientReference(this, referenceFieldName);
    }

    @Override
    public void invalidateDirectoryCache() throws DirectoryException {
        getCache().invalidateAll();
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        // and also invalidates the cache from the source directories
        for (SubDirectoryDescriptor sub : getDescriptor().subDirectories) {
            Directory dir = directoryService.getDirectory(sub.name);
            if (dir != null) {
                dir.invalidateDirectoryCache();
            }

        }
    }

}
