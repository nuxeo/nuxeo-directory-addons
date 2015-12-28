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
 * $Id: SubDirectoryDescriptor.java 24597 2007-09-05 16:04:04Z fguillaume $
 */

package org.nuxeo.ecm.directory.resilient;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Florent Guillaume
 */
@XObject("subDirectory")
public class SubDirectoryDescriptor {

    @XNode("@name")
    public String name;

    @XNode("@master")
    public String master;

    @Override
    public String toString() {
        return String.format("{subdirectory name=%s ", name);
    }

    boolean isMaster() {
        return Boolean.parseBoolean(master);
    }

    /**
     * @since 5.6
     */
    @Override
    public SubDirectoryDescriptor clone() {
        SubDirectoryDescriptor clone = new SubDirectoryDescriptor();
        clone.name = name;
        clone.master = master;
        return clone;
    }
}
