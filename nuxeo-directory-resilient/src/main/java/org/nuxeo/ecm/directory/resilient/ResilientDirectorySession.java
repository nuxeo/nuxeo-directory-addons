/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *
 * $Id: ResilientDirectorySession.java 29556 2008-01-23 00:59:39Z jcarsique $
 */

package org.nuxeo.ecm.directory.resilient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * Directory session aggregating entries from different sources.
 * <p>
 * Each source can build an entry aggregating fields from one or several directories.
 *
 * @author Florent Guillaume
 * @author Anahide Tchertchian
 * @author Maxime Hilaire
 */
public class ResilientDirectorySession extends BaseSession {

    private static final Log log = LogFactory.getLog(ResilientDirectorySession.class);

    private SubDirectoryInfo masterSubDirectoryInfo;

    private List<SubDirectoryInfo> slaveSubDirectoryInfos;

    public ResilientDirectorySession(ResilientDirectory directory) {
        super(directory);
    }

    @Override
    public ResilientDirectory getDirectory() {
        return (ResilientDirectory) directory;
    }

    protected String getSchema() {
        return directory.getSchema();
    }

    protected class SubDirectoryInfo {

        final String dirName;

        final String dirSchemaName;

        final String idField;

        final String passwordField;

        Session session;

        SubDirectoryInfo(String dirName, String dirSchemaName, String idField, String passwordField) {
            this.dirName = dirName;
            this.dirSchemaName = dirSchemaName;
            this.idField = idField;
            this.passwordField = passwordField;
        }

        Session getSession() throws DirectoryException {
            if (session == null) {
                DirectoryService directoryService = Framework.getService(DirectoryService.class);
                session = directoryService.open(dirName);
            }
            return session;
        }

        @Override
        public String toString() {
            return String.format("{directory=%s }", dirName);
        }
    }

    private void init() throws DirectoryException {
        if (masterSubDirectoryInfo == null || (slaveSubDirectoryInfos == null || slaveSubDirectoryInfos.size() == 0)) {
            recomputeSubDirectoryInfos();
        }
    }

    /**
     * Recomputes all the info needed for efficient access.
     */
    private void recomputeSubDirectoryInfos() throws DirectoryException {
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        List<SubDirectoryInfo> newSlaveSubDirectoryInfos = new ArrayList<SubDirectoryInfo>(2);
        for (SubDirectoryDescriptor subDir : getDirectory().getDescriptor().subDirectories) {

            final String dirName = subDir.name;
            final String dirSchemaName = directoryService.getDirectorySchema(dirName);

            final String dirIdField = directoryService.getDirectoryIdField(dirName);
            final String dirPwdField = directoryService.getDirectoryPasswordField(dirName);

            SubDirectoryInfo subDirectoryInfo = new SubDirectoryInfo(dirName, dirSchemaName, dirIdField, dirPwdField);

            if (subDir.isMaster()) {
                if (masterSubDirectoryInfo == null) {
                    masterSubDirectoryInfo = subDirectoryInfo;
                }
            } else {
                newSlaveSubDirectoryInfos.add(subDirectoryInfo);
            }

        }

        slaveSubDirectoryInfos = newSlaveSubDirectoryInfos;
    }

    @Override
    public void close() throws DirectoryException {
        try {
            DirectoryException exc = null;
            if (masterSubDirectoryInfo != null) {
                exc = closeSource(masterSubDirectoryInfo, exc);
            }
            if (slaveSubDirectoryInfos == null) {
                return;
            }

            for (SubDirectoryInfo SubDirectoryInfo : slaveSubDirectoryInfos) {
                exc = closeSource(SubDirectoryInfo, exc);
            }
            if (exc != null) {
                throw exc;
            }

        } finally {
            getDirectory().removeSession(this);
        }
    }

    /**
     * Close a source without throwing exception to let the parent caller deal with the exception returned
     *
     * @param SubDirectoryInfo
     * @param exc
     * @return An exception or null if none
     * @since 5.9
     */
    private DirectoryException closeSource(SubDirectoryInfo subDirectoryInfo, DirectoryException exc) {
        Session session = subDirectoryInfo.session;
        subDirectoryInfo.session = null;
        if (session != null) {
            try {
                session.close();
            } catch (DirectoryException e) {
                // remember exception, we want to close all session
                // first
                if (exc == null) {
                    exc = e;
                } else {
                    // we can't reraise both, log this one
                    log.error("Error closing directory " + subDirectoryInfo.dirName, e);
                }
            }
        }
        return exc;

    }

    /**
     * Get the read-only value
     *
     * @return The value of the read-only mode of the master directory
     * @throws DirectoryException
     *
     */
    @Override
    public boolean isReadOnly() {
        // The aim of this resilient directory is to replicate at least one
        // master directory (read-only or not) to at least one slave
        // If the master directory is in read-only any entry may be created on
        // master, but slave will be replicated in any case
        // So return the value of the master directory, to warn the caller if
        // new entry will be created on master or not
        init();
        return masterSubDirectoryInfo.getSession().isReadOnly();
    }

    /**
     * The method try to create/update the entry if master has it, else delete it from slave If any error, but log warn
     * messages, the aim is not to lock operation when slave are not availale The stuff will be done another time The
     * method works on entry ID (mean the idField is the same on master and slave) and slave don't use auto-increment
     * feature (checked in ResilientDirectory constructor)
     *
     * @param entryId The id of the entry
     * @param fieldMap The list of properties to set in addition of the master one. Can be null when not needed
     * @param masterHasEntry True if the master get it, else false. If flase the entry will be reomved on slave
     */
    private void updateMasterOnSlaves(String entryId, Map<String, Object> fieldMap, boolean masterHasEntry) {
        // if master has entry, update entry on slave, else if it does not exist
        // on slave create it
        // If the master does not have this entry anymore delete it from slave

        if (masterHasEntry) {
            DocumentModel docModel = null;
            try {
                docModel = masterSubDirectoryInfo.getSession().getEntry(entryId);

            } catch (DirectoryException e) {
                log.warn(String.format(
                        "Unable to get the entry id %s on master directory '%s'  while updating slave directory",
                        entryId, masterSubDirectoryInfo.dirName), e);
            }
            if (docModel != null) {
                for (SubDirectoryInfo subDirInfo : slaveSubDirectoryInfos) {
                    try {
                        if (subDirInfo.getSession().hasEntry(entryId)) {
                            final DocumentModel entry = BaseSession.createEntryModel(null, getSchema(), entryId, null);
                            Map<String, Object> masterProps = docModel.getProperties(getSchema());
                            // Force update with the given properties if there are
                            // Some props are not retrieved from master (ex:password)
                            if (fieldMap != null) {
                                masterProps.putAll(fieldMap);
                            } else {
                                if (getPasswordField() != null) {
                                    Field passwordField = getDirectory().getSchemaFieldMap().get(getPasswordField());
                                    if (masterProps.containsKey(passwordField.getName().getPrefixedName())) {
                                        if (masterProps.get(passwordField.getName().getPrefixedName()) == null) {
                                            // The password should be null only
                                            // when
                                            // where are trying to call getEntry
                                            // Remove it to avoid update to null
                                            masterProps.remove(passwordField.getName().getPrefixedName());
                                        }
                                    }
                                }
                            }
                            // Init props from master
                            entry.setProperties(getSchema(), masterProps);
                            subDirInfo.getSession().updateEntry(entry);
                        } else {
                            Map<String, Object> prefixProps = docModel.getProperties(getSchema());
                            // Force update with the given properties if there are
                            // Some props are not retrieved from master (ex:password)
                            if (fieldMap != null) {
                                prefixProps.putAll(fieldMap);
                            }
                            subDirInfo.getSession().createEntry(prefixProps);
                        }
                    }

                    catch (DirectoryException e) {
                        log.warn(String.format("Unable to update the slave directory %s on entry id %s",
                                subDirInfo.dirName, entryId), e);
                    }
                }
            } else {
                log.warn(String.format(
                        "The master directory %s should contains the entry id %s but return null when getting the object",
                        masterSubDirectoryInfo.dirName, entryId));
            }
        } else {
            for (SubDirectoryInfo subDirInfo : slaveSubDirectoryInfos) {
                try {
                    if (subDirInfo.getSession().hasEntry(entryId)) {
                        subDirInfo.getSession().deleteEntry(entryId);
                    }
                }

                catch (DirectoryException e) {
                    log.warn(String.format("Unable to delete the slave directory %s on entry id %s",
                            subDirInfo.dirName, entryId), e);
                }
            }
        }

    }

    private boolean hasEntryOnSlave(String id) {
        init();
        for (SubDirectoryInfo dirInfo : slaveSubDirectoryInfos) {
            Session session = dirInfo.getSession();
            if (session.hasEntry(id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean authenticate(String username, String password) {
        init();

        // First try to authenticate against the master
        try {
            boolean authenticated = masterSubDirectoryInfo.getSession().authenticate(username, password);
            HashMap<String, Object> fieldMap = new HashMap<String, Object>();

            fieldMap.put(getIdField(), username);
            fieldMap.put(getPasswordField(), password);

            updateMasterOnSlaves(username, fieldMap, authenticated);
            return authenticated;
        } catch (DirectoryException e) {
            log.warn(String.format(
                    "Unable to authenticate the user '%s' against the master directory '%s', will fallback on slave",
                    username, masterSubDirectoryInfo.dirName), e);
        }

        // If the master is KO, fallback on slave and try to authenticate
        for (SubDirectoryInfo dirInfo : slaveSubDirectoryInfos) {
            log.info(String.format("Trying to authenticate against slave directory %s", dirInfo.dirName));
            if (dirInfo.getSession().authenticate(username, password)) {
                return true;
            }

        }
        return false;
    }

    @Override
    public DocumentModel getEntry(String id) throws DirectoryException {
        return getEntry(id, true);
    }

    @Override
    /**
     * Get entry on master directory first and fall back on slave(s) when needed
     *
     * @param id
     * @param fetchReferences
     * @return
     * @throws DirectoryException
     *
     */
    public DocumentModel getEntry(String id, boolean fetchReferences) throws DirectoryException {
        init();

        // Try to get the entry in the master first
        // If an exception occurs, catch it, log it and try to get it in the
        // slave

        boolean errorOccurs = false;
        DocumentModel entry = null;
        try {
            entry = masterSubDirectoryInfo.getSession().getEntry(id, fetchReferences);
        } catch (DirectoryException e) {
            log.warn(String.format("Unable to get the entry id '%s' in the directory '%s', will fallback on slave ",
                    id, masterSubDirectoryInfo.dirName), e);
            errorOccurs = true;
        }

        if (entry == null && !errorOccurs) {
            // If the entry is null and no error, remove the entry from
            // slaves
            updateMasterOnSlaves(id, null, false);
        } else if (entry == null && errorOccurs) {
            // Try to get the entry from slaves
            for (SubDirectoryInfo subDirectoryInfo : slaveSubDirectoryInfos) {
                log.info(String.format("Trying to get entry %s on slave directory %s", id, subDirectoryInfo.dirName));
                entry = subDirectoryInfo.getSession().getEntry(id, fetchReferences);
                if (isReadOnly()) {
                    // set readonly the returned entry if the master directory
                    // is in read-only
                    setReadOnlyEntry(entry);
                }
            }

        } else if (entry != null) {
            // Update the entry to the slaves if needed
            updateMasterOnSlaves(entry.getId(), null, true);
        }

        return entry;

    }

    /**
     * Method used for quer and getEntries method This method may raise performance issue Find a smarter way of update
     * Use a cron job that deal with asynchronous update
     *
     * @param masterResults The up-to-date list of results from master
     */
    private void bulkUpdateMasterOnSlave(DocumentModelList masterResults, DocumentModelList slaveResults) {

        // Create/update entries in slave
        for (DocumentModel docModel : masterResults) {
            if (!slaveResults.contains(docModel)) {
                updateMasterOnSlaves(docModel.getId(), null, true);
            }
        }

        // Delete old entries
        for (DocumentModel docModel : slaveResults) {
            if (!masterResults.contains(docModel)) {
                updateMasterOnSlaves(docModel.getId(), null, false);
            }
        }

    }

    @Override
    public DocumentModelList getEntries() {
        throw new UnsupportedOperationException("Get entries may be deprecated !");
    }

    @Override
    public DocumentModel createEntry(Map<String, Object> fieldMap) {
        init();

        if (isReadOnly()) {
            return null;
        }

        Field schemaField = getDirectory().getSchemaFieldMap().get(getIdField());

        final Object rawid = fieldMap.get(schemaField.getName().getPrefixedName());
        if (rawid == null) {
            throw new DirectoryException(String.format("Entry is missing id field '%s'", getIdField()));
        }
        final String id = String.valueOf(rawid); // XXX allow longs too

        final DocumentModel entry = BaseSession.createEntryModel(null, getSchema(), id, null);
        entry.setProperties(getSchema(), fieldMap);

        // Do not fallback if create on master has failed.
        // The master source must stay the most up-to-date source
        masterSubDirectoryInfo.getSession().createEntry(entry);
        updateMasterOnSlaves(id, fieldMap, true);
        return entry;

    }

    @Override
    public void deleteEntry(DocumentModel docModel) {
        deleteEntry(docModel.getId());
    }

    @Override
    public void deleteEntry(String id) {
        init();
        // If we are removing a entry from the master, update the slave(s)
        // even if the master is in read-only mode
        masterSubDirectoryInfo.getSession().deleteEntry(id);
        updateMasterOnSlaves(id, null, false);
    }

    @Override
    public void deleteEntry(String id, Map<String, String> map) throws DirectoryException {
        log.warn("Calling deleteEntry extended on resilient directory");
        deleteEntry(id);
    }

    @Override
    public void updateEntry(DocumentModel docModel) {
        init();
        if (isReadOnly() || isReadOnlyEntry(docModel)) {
            return;
        }

        // Do not fallback if update on master has failed.
        // The master source must stay the most up-to-date source
        masterSubDirectoryInfo.getSession().updateEntry(docModel);
        updateMasterOnSlaves(docModel.getId(), docModel.getProperties(getSchema()), true);

    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter) {
        return query(filter, Collections.<String> emptySet());
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext) {
        return query(filter, fulltext, Collections.<String, String> emptyMap());
    }

    @Override
    @SuppressWarnings("boxing")
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy)
            {
        return query(filter, fulltext, orderBy, false);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences) {
        init();

        // list of entries
        final DocumentModelList results = new DocumentModelListImpl();
        try {
            results.addAll(masterSubDirectoryInfo.getSession().query(filter, fulltext, orderBy, fetchReferences));
            DocumentModelList slaveResults = null;

            for (SubDirectoryInfo subDirectoryInfo : slaveSubDirectoryInfos) {
                try {
                    slaveResults = subDirectoryInfo.getSession().query(filter, fulltext, orderBy, fetchReferences);

                    // XXX : Should we update all entries on all directories ??
                    // Performance issue ...
                    // =>Cron job ? see getEntries for a common solution ?
                    bulkUpdateMasterOnSlave(results, slaveResults);

                } catch (DirectoryException exc) {
                    log.warn(
                            String.format(
                                    "Resilient directory '%s' : Unable to query entries on slave directory '%s'for synchronization",
                                    directory.getName(), masterSubDirectoryInfo.dirName), exc);
                }
            }
        } catch (DirectoryException e) {
            log.warn(String.format(
                    "Resilient directory '%s' : Unable to query entries on master directory '%s', fallback on slaves",
                    directory.getName(), masterSubDirectoryInfo.dirName), e);

            // Try to get the entry from slaves
            for (SubDirectoryInfo subDirectoryInfo : slaveSubDirectoryInfos) {
                try {
                    results.addAll(subDirectoryInfo.getSession().query(filter, fulltext, orderBy, fetchReferences));
                    break;
                } catch (DirectoryException exc) {
                    log.warn(
                            String.format(
                                    "Resilient directory '%s' : Unable to query entries on slave directory '%s', fallback on another slave if it exists",
                                    directory.getName(), masterSubDirectoryInfo.dirName), e);
                }
            }

        }

        for (DocumentModel documentModel : results) {
            if (isReadOnly()) {
                setReadOnlyEntry(documentModel);
            }

        }
        return results;

    }

    @Override
    public List<String> getProjection(Map<String, Serializable> filter, String columnName) {
        return getProjection(filter, Collections.<String> emptySet(), columnName);
    }

    @Override
    public List<String> getProjection(Map<String, Serializable> filter, Set<String> fulltext, String columnName)
            {

        final DocumentModelList entries = query(filter, fulltext);
        final List<String> results = new ArrayList<String>(entries.size());
        for (DocumentModel entry : entries) {
            final Object value = entry.getProperty(getSchema(), columnName);
            if (value == null) {
                results.add(null);
            } else {
                results.add(value.toString());
            }
        }
        return results;
    }

    @Override
    public DocumentModel createEntry(DocumentModel entry) {
        Map<String, Object> fieldMap = entry.getProperties(getSchema());
        return createEntry(fieldMap);
    }

    @Override
    public boolean hasEntry(String id) {
        init();
        try {
            boolean masterHasEntry = masterSubDirectoryInfo.getSession().hasEntry(id);
            updateMasterOnSlaves(id, null, masterHasEntry);
            return masterHasEntry;
        } catch (DirectoryException e) {
            log.warn(String.format(
                    "Unable to check if master directory '%s' has entry id '%s', fallback check on slaves ...",
                    masterSubDirectoryInfo.dirName, id), e);
            return hasEntryOnSlave(id);
        }
    }

}
