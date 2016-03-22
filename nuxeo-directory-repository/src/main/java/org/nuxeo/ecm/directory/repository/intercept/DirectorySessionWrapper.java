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
package org.nuxeo.ecm.directory.repository.intercept;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.repository.RepositoryDirectory;

public interface DirectorySessionWrapper {

    void init(WrappableDirectorySession session, RepositoryDirectory repositoryDirectory);

    DocumentModel getEntry(String id, boolean fetchReferences) throws DirectoryException;

    DocumentModel createEntry(Map<String, Object> fieldMap) throws DirectoryException;

    void updateEntry(DocumentModel docModel) throws DirectoryException;

    void deleteEntry(String id);

    DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences, int limit, int offset) throws DirectoryException;

    boolean authenticate(String username, String password);

}
