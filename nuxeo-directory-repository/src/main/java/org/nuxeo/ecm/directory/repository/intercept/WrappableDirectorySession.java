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

    DocumentModel doCreateEntry(Map<String, Object> fieldMap) throws ClientException, DirectoryException;

    void doUpdateEntry(DocumentModel docModel) throws ClientException, DirectoryException;

    void doDeleteEntry(String id) throws ClientException;

    DocumentModelList doQuery(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences, int limit, int offset) throws ClientException, DirectoryException;

    boolean doAuthenticate(String username, String password) throws ClientException;

}
