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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor;
import org.nuxeo.ecm.directory.InverseReference;
import org.nuxeo.ecm.directory.InverseReferenceDescriptor;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.ReferenceDescriptor;

@XObject("directory")
public class ConnectorBasedDirectoryDescriptor extends BaseDirectoryDescriptor {

    protected Log log = LogFactory.getLog(ConnectorBasedDirectoryDescriptor.class);

    @XNode("@class")
    protected Class<? extends EntryConnector> connectorClass;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> parameters = new HashMap<String, String>();

    @XNodeMap(value = "mapping/map", key = "@field", type = HashMap.class, componentType = String.class)
    protected Map<String, String> mapping = new HashMap<String, String>();

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

    public Map<String, String> getMapping() {
        return mapping;
    }

    @Override
    public void merge(BaseDirectoryDescriptor other) {
        super.merge(other);
        if (other instanceof ConnectorBasedDirectoryDescriptor) {
            merge((ConnectorBasedDirectoryDescriptor) other);
        }
    }

    protected void merge(ConnectorBasedDirectoryDescriptor other) {
        super.merge(other);
        if (other.connectorClass != null) {
            connectorClass = other.connectorClass;
            connector = null;
        }
        if (other.parameters != null) {
            parameters = other.parameters;
        }
        if (other.mapping != null) {
            mapping.putAll(other.mapping);
        }
    }

    @Override
    public ConnectorBasedDirectoryDescriptor clone() {
        ConnectorBasedDirectoryDescriptor clone = (ConnectorBasedDirectoryDescriptor) super.clone();
        // basic fields are already copied by super.clone()
        if (parameters != null) {
            clone.parameters = new HashMap<>(parameters);
        }
        if (mapping != null) {
            clone.mapping = new HashMap<>(mapping);
        }
        return clone;
    }

    @Override
    public ConnectorBasedDirectory newDirectory() {
        return new ConnectorBasedDirectory(this);
    }

}
