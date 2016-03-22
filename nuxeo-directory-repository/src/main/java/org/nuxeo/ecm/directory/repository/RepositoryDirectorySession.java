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
 *     Maxime Hilaire
 *
 */
package org.nuxeo.ecm.directory.repository;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.digest.PasswordHelper;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.repository.intercept.DirectorySessionWrapper;
import org.nuxeo.ecm.directory.repository.intercept.WrappableDirectorySession;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;

/**
 * Session class for directory on repository
 *
 * @since 5.9.6
 */
public class RepositoryDirectorySession extends BaseSession implements WrappableDirectorySession {

    final protected String schemaName;

    final protected String schemaIdField;

    final protected String schemaPasswordField;

    final protected CoreSession coreSession;

    final protected String createPath;

    final protected String docType;

    protected static final String UUID_FIELD = "ecm:uuid";

    private final static Log log = LogFactory.getLog(RepositoryDirectorySession.class);

    protected DirectorySessionWrapper wrapper;

    public RepositoryDirectorySession(RepositoryDirectory directory) {
        super(directory);
        schemaName = directory.getSchema();
        RepositoryDirectoryDescriptor descriptor = directory.getDescriptor();
        coreSession = CoreInstance.openCoreSession(descriptor.getRepositoryName());
        schemaIdField = directory.getFieldMapper().getBackendField(getIdField());
        schemaPasswordField = directory.getFieldMapper().getBackendField(getPasswordField());
        docType = descriptor.docType;
        createPath = descriptor.getCreatePath();

        // init wrapper
        wrapper = descriptor.getWrapper();
        wrapper.init(this, getDirectory());
    }

    @Override
    public RepositoryDirectory getDirectory() {
        return (RepositoryDirectory) directory;
    }

    @Override
    public DocumentModel getEntry(String id) throws DirectoryException {
        return getEntry(id, false);
    }

    @Override
    public DocumentModel getEntry(String id, boolean fetchReferences) throws DirectoryException {
        return wrapper.getEntry(id, fetchReferences);
    }

    @Override
    public DocumentModel doGetEntry(String id, boolean fetchReferences) throws DirectoryException {

        if (UUID_FIELD.equals(getIdField())) {
            IdRef ref = new IdRef(id);
            if (coreSession.exists(ref)) {
                DocumentModel document = coreSession.getDocument(new IdRef(id));
                return docType.equals(document.getType()) ? document : null;
            } else {
                return null;
            }
        }

        StringBuilder sbQuery = new StringBuilder("SELECT * FROM ");
        sbQuery.append(docType);
        sbQuery.append(" WHERE ");
        sbQuery.append(getDirectory().getField(schemaIdField).getName().getPrefixedName());
        sbQuery.append(" = '");
        sbQuery.append(id);
        sbQuery.append("' AND ecm:path STARTSWITH '");
        sbQuery.append(createPath);
        sbQuery.append("'");

        DocumentModelList listDoc = coreSession.query(sbQuery.toString());
        // TODO : deal with references
        if (!listDoc.isEmpty()) {
            // Should have only one
            if (listDoc.size() > 1) {
                log.warn(String.format("Found more than one result in getEntry, the first result only will be returned"));
            }
            DocumentModel docResult = listDoc.get(0);
            if (isReadOnly()) {
                BaseSession.setReadOnlyEntry(docResult);
            }
            return docResult;
        }
        return null;
    }

    @Override
    public DocumentModelList getEntries() throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    private String getPrefixedFieldName(String fieldName) {
        if (UUID_FIELD.equals(fieldName)) {
            return fieldName;
        }
        Field schemaField = getDirectory().getField(fieldName);
        return schemaField.getName().getPrefixedName();
    }

    @Override
    public DocumentModel createEntry(Map<String, Object> fieldMap) throws DirectoryException {
        return wrapper.createEntry(fieldMap);
    }

    @Override
    @SuppressWarnings("unchecked")
    public DocumentModel doCreateEntry(Map<String, Object> fieldMap) throws DirectoryException {

        if (isReadOnly()) {
            log.warn(String.format("The directory '%s' is in read-only mode, could not create entry.",
                    directory.getName()));
            return null;
        }
        // TODO : deal with auto-versionning
        // TODO : deal with encrypted password
        // TODO : deal with references
        Map<String, Object> properties = new HashMap<String, Object>();
        List<String> createdRefs = new LinkedList<String>();
        for (String fieldId : fieldMap.keySet()) {
            if (getDirectory().isReference(fieldId)) {
                createdRefs.add(fieldId);
            }
            Object value = fieldMap.get(fieldId);
            properties.put(getMappedPrefixedFieldName(fieldId), value);
        }

        final String rawid = (String) properties.get(getPrefixedFieldName(schemaIdField));
        if (rawid == null && (!UUID_FIELD.equals(getIdField()))) {
            throw new DirectoryException(String.format("Entry is missing id field '%s'", schemaIdField));
        }

        DocumentModel docModel = coreSession.createDocumentModel(createPath, rawid, docType);

        docModel.setProperties(schemaName, properties);
        DocumentModel createdDoc = coreSession.createDocument(docModel);

        for (String referenceFieldName : createdRefs) {
            Reference reference = directory.getReference(referenceFieldName);
            List<String> targetIds = (List<String>) createdDoc.getProperty(schemaName, referenceFieldName);
            reference.setTargetIdsForSource(docModel.getId(), targetIds);
        }
        return docModel;
    }

    @Override
    public void updateEntry(DocumentModel docModel) throws DirectoryException {
        wrapper.updateEntry(docModel);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void doUpdateEntry(DocumentModel docModel) throws DirectoryException {
        if (isReadOnly()) {
            log.warn(String.format("The directory '%s' is in read-only mode, could not update entry.",
                    directory.getName()));
        } else {

            if (!isReadOnlyEntry(docModel)) {

                String id = (String) docModel.getProperty(schemaName, getIdField());
                if (id == null) {
                    throw new DirectoryException("Can not update entry with a null id for document ref "
                            + docModel.getRef());
                } else {
                    if (getEntry(id) == null) {
                        throw new DirectoryException(String.format(
                                "Update entry failed : Entry with id '%s' not found !", id));
                    } else {

                        DataModel dataModel = docModel.getDataModel(schemaName);
                        Map<String, Object> updatedProps = new HashMap<String, Object>();
                        List<String> updatedRefs = new LinkedList<String>();

                        for (String field : docModel.getProperties(schemaName).keySet()) {
                            String schemaField = getMappedPrefixedFieldName(field);
                            if (!dataModel.isDirty(schemaField)) {
                                if (getDirectory().isReference(field)) {
                                    updatedRefs.add(field);
                                } else {
                                    updatedProps.put(schemaField, docModel.getProperties(schemaName).get(field));
                                }
                            }

                        }

                        docModel.setProperties(schemaName, updatedProps);

                        // update reference fields
                        for (String referenceFieldName : updatedRefs) {
                            Reference reference = directory.getReference(referenceFieldName);
                            List<String> targetIds = (List<String>) docModel.getProperty(schemaName, referenceFieldName);
                            reference.setTargetIdsForSource(docModel.getId(), targetIds);
                        }

                        coreSession.saveDocument(docModel);
                    }

                }
            }
        }
    }

    @Override
    public void deleteEntry(DocumentModel docModel) throws DirectoryException {
        String id = (String) docModel.getProperty(schemaName, schemaIdField);
        deleteEntry(id);
    }

    @Override
    public void deleteEntry(String id) throws DirectoryException {
        wrapper.deleteEntry(id);
    }

    @Override
    public void doDeleteEntry(String id) throws DirectoryException {
        if (isReadOnly()) {
            log.warn(String.format("The directory '%s' is in read-only mode, could not delete entry.",
                    directory.getName()));
        } else {
            if (id == null) {
                throw new DirectoryException("Can not update entry with a null id ");
            } else {
                DocumentModel docModel = getEntry(id);
                if (docModel != null) {
                    coreSession.removeDocument(docModel.getRef());
                }
            }
        }
    }

    @Override
    public void deleteEntry(String id, Map<String, String> map) throws DirectoryException {
        if (isReadOnly()) {
            log.warn(String.format("The directory '%s' is in read-only mode, could not delete entry.",
                    directory.getName()));
        }

        Map<String, Serializable> props = new HashMap<String, Serializable>(map);
        props.put(schemaIdField, id);

        DocumentModelList docList = query(props);
        if (!docList.isEmpty()) {
            if (docList.size() > 1) {
                log.warn(String.format("Found more than one result in getEntry, the first result only will be deleted"));
            }
            deleteEntry(docList.get(0));
        } else {
            throw new DirectoryException(String.format("Delete entry failed : Entry with id '%s' not found !", id));
        }

    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter) {
        Set<String> emptySet = Collections.emptySet();
        return query(filter, emptySet);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy)
            {
        // XXX not fetch references by default: breaks current behavior
        return query(filter, fulltext, orderBy, false);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences) {
        return query(filter, fulltext, orderBy, fetchReferences, 0, 0);
    }

    protected String getMappedPrefixedFieldName(String fieldName) {
        String backendFieldId = getDirectory().getFieldMapper().getBackendField(fieldName);
        return getPrefixedFieldName(backendFieldId);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences, int limit, int offset) throws DirectoryException {
        return wrapper.query(filter, fulltext, orderBy, fetchReferences, limit, offset);
    }

    @Override
    public DocumentModelList doQuery(final Map<String, Serializable> filter, final Set<String> fulltext,
            Map<String, String> orderBy, boolean fetchReferences, int limit, int offset) throws
            DirectoryException {
        StringBuilder sbQuery = new StringBuilder("SELECT * FROM ");
        sbQuery.append(docType);
        // TODO deal with fetch ref
        if (!filter.isEmpty() || !fulltext.isEmpty() || (createPath != null && !createPath.isEmpty())) {
            sbQuery.append(" WHERE ");
        }
        int i = 1;
        boolean hasFilter = false;
        for (String filterKey : filter.keySet()) {
            if (!fulltext.contains(filterKey)) {
                sbQuery.append(getMappedPrefixedFieldName(filterKey));
                sbQuery.append(" = ");
                sbQuery.append("'");
                sbQuery.append(filter.get(filterKey));
                sbQuery.append("'");
                if (i < filter.size()) {
                    sbQuery.append(" AND ");
                    i++;
                }
                hasFilter = true;
            }

        }
        if (hasFilter && filter.size() > 0 && fulltext.size() > 0) {
            sbQuery.append(" AND ");
        }
        if (fulltext.size() > 0) {

            Collection<String> fullTextValues = Collections2.transform(fulltext, new Function<String, String>() {

                @Override
                public String apply(String key) {
                    return (String) filter.get(key);
                }

            });
            sbQuery.append("ecm:fulltext");
            sbQuery.append(" = ");
            sbQuery.append("'");
            sbQuery.append(Joiner.on(" ").join(fullTextValues));
            sbQuery.append("'");
        }

        if ((createPath != null && !createPath.isEmpty())) {
            if (filter.size() > 0 || fulltext.size() > 0) {
                sbQuery.append(" AND ");
            }
            sbQuery.append(" ecm:path STARTSWITH '");
            sbQuery.append(createPath);
            sbQuery.append("'");
        }

        // Filter facetFilter = new FacetFilter(FacetNames.VERSIONABLE, true);

        DocumentModelList resultsDoc = coreSession.query(sbQuery.toString(), null, new Long(limit), new Long(offset),
                false);

        if (isReadOnly()) {
            for (DocumentModel documentModel : resultsDoc) {
                BaseSession.setReadOnlyEntry(documentModel);
            }
        }
        return resultsDoc;

    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext) throws
            DirectoryException {
        return query(filter, fulltext, new HashMap<String, String>());
    }

    @Override
    public void close() throws DirectoryException {
        coreSession.close();
        getDirectory().removeSession(this);
    }

    @Override
    public List<String> getProjection(Map<String, Serializable> filter, String columnName) throws
            DirectoryException {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getProjection(Map<String, Serializable> filter, Set<String> fulltext, String columnName)
            throws DirectoryException {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean authenticate(String username, String password) {
        return wrapper.authenticate(username, password);
    }

    @Override
    public boolean doAuthenticate(String username, String password) {
        DocumentModel entry = getEntry(username);
        if (entry == null) {
            return false;
        }
        String storedPassword = (String) entry.getProperty(schemaName, schemaPasswordField);
        return PasswordHelper.verifyPassword(password, storedPassword);
    }

    @Override
    public boolean isAuthenticating() {
        return schemaPasswordField != null;
    }

    @Override
    public boolean hasEntry(String id) {
        return getEntry(id) != null;
    }

    @Override
    public DocumentModel createEntry(DocumentModel entry) {
        Map<String, Object> fieldMap = entry.getProperties(schemaName);
        return createEntry(fieldMap);
    }

}
