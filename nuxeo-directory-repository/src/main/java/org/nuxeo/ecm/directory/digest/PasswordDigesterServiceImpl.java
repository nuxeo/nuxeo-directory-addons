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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 7.1
 */
public class PasswordDigesterServiceImpl extends DefaultComponent implements PasswordDigesterService {

    private static final Pattern HASH_PATTERN = Pattern.compile("^\\{(.*)\\}(.*)$");

    private static final String DIGESTER_XP_NAME = "digester";

    Map<String, PasswordDigester> digesters = new ConcurrentHashMap<>();

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (DIGESTER_XP_NAME.equals(extensionPoint)) {
            PasswordDigesterDescriptor pdd = (PasswordDigesterDescriptor) contribution;

            if (pdd.enabled) {
                digesters.put(pdd.name, pdd.buildDigester());
            } else if (digesters.containsKey(pdd.name)) {
                digesters.remove(pdd.name);
            }

        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (DIGESTER_XP_NAME.equals(extensionPoint)) {
            PasswordDigesterDescriptor pdd = (PasswordDigesterDescriptor) contribution;
            if (digesters.containsKey(pdd.name)) {
                digesters.remove(pdd.name);
            }
        }
    }

    @Override
    public PasswordDigester getPasswordDigester(String name) throws UnknownAlgorithmException {
        if (digesters.containsKey(name)) {
            return digesters.get(name);
        } else {
            throw new UnknownAlgorithmException();
        }
    }

    /**
     * @param hashedPassword
     * @return
     */
    @Override
    public String getDigesterNameFromHash(String hashedPassword) {
        Matcher m = HASH_PATTERN.matcher(hashedPassword);
        try {
            return m.matches() ? m.group(1) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
