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
