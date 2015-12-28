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
 * $Id: MultiDirectoryDescriptor.java 24597 2007-09-05 16:04:04Z fguillaume $
 */

package org.nuxeo.ecm.directory.resilient;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Florent Guillaume
 * @author Maxime Hilaire
 */
@XObject(value = "directory")
public class ResilientDirectoryDescriptor implements Cloneable {

    @XNode("@name")
    public String name;

    @XNode("@remove")
    public boolean remove = false;

    @XNodeList(value = "subDirectory", type = SubDirectoryDescriptor[].class, componentType = SubDirectoryDescriptor.class)
    protected SubDirectoryDescriptor[] subDirectories;

    /**
     * @since 5.6
     */
    @Override
    public ResilientDirectoryDescriptor clone() {
        ResilientDirectoryDescriptor clone = new ResilientDirectoryDescriptor();
        clone.name = name;
        clone.remove = remove;
        if (subDirectories != null) {
            clone.subDirectories = new SubDirectoryDescriptor[subDirectories.length];
            for (int i = 0; i < subDirectories.length; i++) {
                clone.subDirectories[i] = subDirectories[i].clone();
            }
        }
        return clone;
    }

}
