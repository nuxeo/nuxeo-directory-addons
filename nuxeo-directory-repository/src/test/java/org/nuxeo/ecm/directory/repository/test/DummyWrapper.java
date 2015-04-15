package org.nuxeo.ecm.directory.repository.test;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.repository.intercept.DirectorySessionWrapper;
import org.nuxeo.ecm.directory.repository.intercept.SimpleForward;

public class DummyWrapper extends SimpleForward implements DirectorySessionWrapper {

    @Override
    public DocumentModel getEntry(String id, boolean fetchReferences) throws DirectoryException {

        if ("dummy".equals(id)) {
            id = RepositoryDirectoryInit.DOC_ID_USER1;
        }

        return super.getEntry(id, fetchReferences);
    }

}
