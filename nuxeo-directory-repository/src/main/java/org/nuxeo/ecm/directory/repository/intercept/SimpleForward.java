package org.nuxeo.ecm.directory.repository.intercept;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.repository.RepositoryDirectory;

public class SimpleForward implements DirectorySessionWrapper {

    protected WrappableDirectorySession wrappedSession;

    protected RepositoryDirectory repositoryDirectory;

    @Override
    public void init(WrappableDirectorySession session, RepositoryDirectory repositoryDirectory) {
        this.wrappedSession = session;
        this.repositoryDirectory = repositoryDirectory;
    }

    @Override
    public DocumentModel getEntry(String id, boolean fetchReferences) throws DirectoryException {
        return wrappedSession.doGetEntry(id, fetchReferences);
    }

    @Override
    public DocumentModel createEntry(Map<String, Object> fieldMap) throws ClientException, DirectoryException {
        return wrappedSession.doCreateEntry(fieldMap);
    }

    @Override
    public void updateEntry(DocumentModel docModel) throws ClientException, DirectoryException {
        wrappedSession.doUpdateEntry(docModel);
    }

    @Override
    public void deleteEntry(String id) throws ClientException {
        wrappedSession.doDeleteEntry(id);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences, int limit, int offset) throws ClientException, DirectoryException {
        return wrappedSession.doQuery(filter, fulltext, orderBy, fetchReferences, limit, offset);
    }

    @Override
    public boolean authenticate(String username, String password) throws ClientException {
        return wrappedSession.doAuthenticate(username, password);
    }

}
