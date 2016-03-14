/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.resilient;

import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor;
import org.nuxeo.ecm.directory.Directory;

/**
 * Resilient directory descriptor.
 */
@XObject(value = "directory")
public class ResilientDirectoryDescriptor extends BaseDirectoryDescriptor {

    @XNodeList(value = "subDirectory", type = SubDirectoryDescriptor[].class, componentType = SubDirectoryDescriptor.class)
    protected SubDirectoryDescriptor[] subDirectories;

    @Override
    public void merge(BaseDirectoryDescriptor other) {
        super.merge(other);
        merge((ResilientDirectoryDescriptor) other);
    }

    protected void merge(ResilientDirectoryDescriptor other) {
        if (other.subDirectories != null) {
            subDirectories = other.subDirectories.clone();
        }
    }

    /**
     * @since 5.6
     */
    @Override
    public ResilientDirectoryDescriptor clone() {
        ResilientDirectoryDescriptor clone = (ResilientDirectoryDescriptor) super.clone();
        // basic fields are already copied by super.clone()
        if (subDirectories != null) {
            clone.subDirectories = new SubDirectoryDescriptor[subDirectories.length];
            for (int i = 0; i < subDirectories.length; i++) {
                clone.subDirectories[i] = subDirectories[i].clone();
            }
        }
        return clone;
    }

    @Override
    public ResilientDirectory newDirectory() {
        return new ResilientDirectory(this);
    }

}
