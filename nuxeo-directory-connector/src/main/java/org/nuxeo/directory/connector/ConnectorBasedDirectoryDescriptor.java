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
 *     Thierry Delprat
 */
package org.nuxeo.directory.connector;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.InverseReference;
import org.nuxeo.ecm.directory.Reference;

@XObject("directory")
public class ConnectorBasedDirectoryDescriptor implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected Log log = LogFactory.getLog(ConnectorBasedDirectoryDescriptor.class);

    @XNode("@name")
    protected String name;

    @XNode("schema")
    protected String schema;

    @XNode("@class")
    protected Class<? extends EntryConnector> connectorClass;

    @XNode("idField")
    public String idField;

    @XNode("passwordField")
    private String passwordField;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> parameters = new HashMap<String, String>();

    @XNodeList(value = "references/inverseReference", type = InverseReference[].class, componentType = InverseReference.class)
    private InverseReference[] inverseReferences;

    @XNodeMap(value = "mapping/map", key = "@field", type = HashMap.class, componentType = String.class)
    protected Map<String, String> mapping = new HashMap<String, String>();

    public Reference[] getInverseReferences() {
        return inverseReferences;
    }

    public Reference[] getTableReferences() {
        return null;
    }

    protected EntryConnector connector = null;

    public EntryConnector getConnector() {
        if (connector == null) {
            try {
                connector = (EntryConnector) connectorClass.newInstance();
                connector.init(this);
            } catch (ReflectiveOperationException e) {
                log.error("Unable to get connector", e);
            }
        }
        return connector;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getName() {
        return name;
    }

    public String getSchemaName() {
        return schema;
    }

    public String getIdField() {
        return idField;
    }

    public String getPasswordField() {
        return passwordField;
    }

    public Map<String, String> getMapping() {
        return mapping;
    }

}
