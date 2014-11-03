/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 *
 * $Id: MultiDirectory.java 25713 2007-10-05 16:06:58Z fguillaume $
 */

package org.nuxeo.ecm.directory.resilient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
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
 *
 */
public class ResilientDirectory extends AbstractDirectory {

    private String schemaName = null;

    private String idField = null;

    private String passwordField = null;

    private final ResilientDirectoryDescriptor descriptor;

    private Map<String, Field> schemaFieldMap;

    public ResilientDirectory(ResilientDirectoryDescriptor descriptor)
            throws ClientException {
        super(descriptor.name);
        this.descriptor = descriptor;

    }

    private boolean checkSlaveSubDirectory(String masterSchemaName)
            throws ClientException {
        boolean slaveFound = false;
        for (SubDirectoryDescriptor sub : descriptor.subDirectories) {
            Directory subDir = ResilientDirectoryFactory.getDirectoryService().getDirectory(
                    sub.name);
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
                                    descriptor.name, subDir.getName(),
                                    masterSchemaName));
                } else if (!subDir.getIdField().equalsIgnoreCase(idField)
                        && !subDir.getPasswordField().equalsIgnoreCase(
                                passwordField)) {
                    throw new DirectoryException(
                            String.format(
                                    "ResilientDirectory '%s' reference a slave directory '%s' that has not the same idField/passwordField than the master !",
                                    descriptor.name, subDir.getName()));
                }

                else if (subDir.getSession().isReadOnly()) {
                    throw new DirectoryException(
                            String.format(
                                    "ResilientDirectory '%s' reference a slave directory '%s' that is in read-only mode !",
                                    descriptor.name, subDir.getName()));
                } else if (subDir instanceof SQLDirectory) {
                    if (((SQLDirectory) subDir).getConfig().autoincrementIdField) {
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
        for (SubDirectoryDescriptor sub : descriptor.subDirectories) {
            Directory dir = Framework.getLocalService(DirectoryService.class).getDirectory(
                    sub.name);
            if (sub.isMaster() && masterSchemaName == null) {
                if (dir != null) {
                    schemaName = dir.getSchema();
                    idField = dir.getIdField();
                    passwordField = dir.getPasswordField();
                    SchemaManager sm = Framework.getLocalService(SchemaManager.class);
                    Schema sch = sm.getSchema(schemaName);
                    if (sch == null) {
                        throw new DirectoryException(
                                String.format(
                                        "Unknown schema '%s' for master subdirectory '%s' ",
                                        schemaName, dir.getName()));
                    }
                    final Set<String> sourceFields = new HashSet<String>();
                    for (Field f : sch.getFields()) {
                        sourceFields.add(f.getName().getLocalName());
                    }
                    if (!sourceFields.contains(idField)) {
                        throw new DirectoryException(
                                String.format(
                                        "Directory '%s' schema '%s' has no id field '%s'",
                                        dir.getName(), schemaName, idField));
                    }
                    schemaFieldMap = new HashMap<String, Field>();
                    for (Field f : sch.getFields()) {
                        String fieldName = f.getName().getLocalName();
                        schemaFieldMap.put(fieldName, f);
                    }

                    masterSchemaName = schemaName;

                } else {
                    throw new DirectoryException(String.format(
                            "Unknown directory '%s' !", sub.name));
                }
            } else if (sub.isMaster() && masterSchemaName != null) {
                throw new DirectoryException(
                        String.format(
                                "Directory '%s' subdir '%s' "
                                        + "define a master source but another one exist !",
                                descriptor.name, sub.name));
            }

        }
        // At this point we must have found a master
        if (masterSchemaName == null) {
            throw new DirectoryException(String.format(
                    "Resilient Directory '%s' has no master source !",
                    descriptor.name));
        } else {
            if (!checkSlaveSubDirectory(masterSchemaName)) {
                throw new DirectoryException(String.format(
                        "Resilient Directory '%s' has no slave source !",
                        descriptor.name));

            }
        }

    }

    protected ResilientDirectoryDescriptor getDescriptor() {
        return descriptor;
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

    protected void addSession(ResilientDirectorySession session) {
        sessions.add(session);
    }

    @Override
    public Reference getReference(String referenceFieldName) {
        return new ResilientReference(this, referenceFieldName);
    }

    @Override
    public void invalidateDirectoryCache() throws DirectoryException {
        getCache().invalidateAll();
        // and also invalidates the cache from the source directories
        for (SubDirectoryDescriptor sub : descriptor.subDirectories) {
            Directory dir = ResilientDirectoryFactory.getDirectoryService().getDirectory(
                    sub.name);
            if (dir != null) {
                dir.invalidateDirectoryCache();
            }

        }
    }

}
