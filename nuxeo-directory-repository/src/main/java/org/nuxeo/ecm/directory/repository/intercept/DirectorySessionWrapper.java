package org.nuxeo.ecm.directory.repository.intercept;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
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
