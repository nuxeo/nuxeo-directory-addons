package org.nuxeo.ecm.directory.repository.intercept;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectoryException;

public interface WrappableDirectorySession {

    DocumentModel doGetEntry(String id, boolean fetchReferences) throws DirectoryException;

    DocumentModel doCreateEntry(Map<String, Object> fieldMap) throws DirectoryException;

    void doUpdateEntry(DocumentModel docModel) throws DirectoryException;

    void doDeleteEntry(String id);

    DocumentModelList doQuery(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences, int limit, int offset) throws DirectoryException;

    boolean doAuthenticate(String username, String password);

}
