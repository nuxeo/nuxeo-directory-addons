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
 *     dmetzler
 */
package org.nuxeo.ecm.directory.digest;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 7.1
 */
@XObject("digester")
public class PasswordDigesterDescriptor {

    @XNode("@name")
    String name;

    @XNode("@enabled")
    boolean enabled = true;

    @XNode("@class")
    Class<PasswordDigester> digesterKlass;

    @XNodeMap(value = "params/param", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> params;

    public PasswordDigester buildDigester() {
        try {
            PasswordDigester digester = digesterKlass.newInstance();
            digester.setName(name);
            digester.setParams(params);
            return digester;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Unable to build digester " + name);
        }
    }

    @Override
    public String toString() {
        return String.format("Digester(%s)[class=%s,enabled=%s]", name, digesterKlass.getName(), enabled ? "true"
                : "false");
    }

}
