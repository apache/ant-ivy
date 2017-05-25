/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.plugins.resolver;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvyPattern;
import org.apache.ivy.plugins.repository.file.FileRepository;
import org.apache.ivy.util.Checks;
import org.apache.ivy.util.Message;

/**
 */
public class FileSystemResolver extends RepositoryResolver {

    private static final String TRANSACTION_DESTINATION_SUFFIX = ".part";

    private static final Pattern TRANSACTION_PATTERN = Pattern
            .compile("(.*[/\\\\]\\[revision\\])([/\\\\].+)");

    /**
     * Transactional mode.
     * 
     * auto: use transaction if possible, only log verbose message if not true: always use
     * transaction, fail if not supported false: never use transactions
     */
    private String transactional = "auto"; // one of 'auto', 'true' or 'false'

    /**
     * When set indicates if this resolver supports transaction
     */
    private Boolean supportTransaction;

    /**
     * The pattern leading to the directory where files are published before being moved at the end
     * of a transaction
     */
    private String baseTransactionPattern;

    /**
     * Map between actual patterns and patterns used during the transaction to put files in a
     * temporary directory
     */
    private Map<String, String> fullTransactionPatterns = new HashMap<String, String>();

    /**
     * Location where files are published during the transaction
     */
    private File transactionTempDir;

    /**
     * Location where files should end up at the end of the transaction
     */
    private File transactionDestDir;

    public FileSystemResolver() {
        setRepository(new FileRepository());
    }

    @Override
    public String getTypeName() {
        return "file";
    }

    public boolean isLocal() {
        return getFileRepository().isLocal();
    }

    public void setLocal(boolean local) {
        getFileRepository().setLocal(local);
    }

    private FileRepository getFileRepository() {
        return (FileRepository) getRepository();
    }

    @Override
    protected String getDestination(String pattern, Artifact artifact, ModuleRevisionId mrid) {
        if (supportTransaction() && isTransactionStarted()) {

            String destPattern = fullTransactionPatterns.get(pattern);
            if (destPattern == null) {
                throw new IllegalArgumentException(
                        "unsupported pattern for publish destination pattern: " + pattern
                                + ". supported patterns: " + fullTransactionPatterns.keySet());
            }
            return IvyPatternHelper.substitute(destPattern, mrid, artifact);
        } else {
            return super.getDestination(pattern, artifact, mrid);
        }
    }

    private boolean isTransactionStarted() {
        return transactionTempDir != null;
    }

    @Override
    public void abortPublishTransaction() throws IOException {
        if (supportTransaction()) {
            if (isTransactionStarted()) {
                try {
                    getFileRepository().delete(transactionTempDir);
                    Message.info("\tpublish aborted: deleted " + transactionTempDir);
                } finally {
                    closeTransaction();
                }
            } else {
                Message.info("\tpublish aborted: nothing was started");
            }
        }
    }

    @Override
    public void commitPublishTransaction() throws IOException {
        if (supportTransaction()) {
            if (!isTransactionStarted()) {
                throw new IllegalStateException("no current transaction!");
            }
            if (transactionDestDir.exists()) {
                throw new IOException(
                        "impossible to commit transaction: transaction destination directory "
                                + "already exists: "
                                + transactionDestDir
                                + "\npossible cause: usage of identifying tokens after the revision token");
            }
            try {
                getFileRepository().move(transactionTempDir, transactionDestDir);

                Message.info("\tpublish commited: moved " + transactionTempDir + " \n\t\tto "
                        + transactionDestDir);
            } catch (IOException ex) {
                IOException commitEx;
                try {
                    getFileRepository().delete(transactionTempDir);
                    commitEx = new IOException("publish transaction commit error for "
                            + transactionDestDir + ": rolled back");
                } catch (IOException deleteEx) {
                    commitEx = new IOException("publish transaction commit error for "
                            + transactionDestDir + ": rollback impossible either, "
                            + "please remove " + transactionTempDir + " manually");
                }
                commitEx.initCause(ex);
                throw commitEx;
            } finally {
                closeTransaction();
            }
        }
    }

    @Override
    public void beginPublishTransaction(ModuleRevisionId module, boolean overwrite)
            throws IOException {
        if (supportTransaction()) {
            if (isTransactionStarted()) {
                throw new IllegalStateException("a transaction is already started and not closed!");
            }
            if (overwrite) {
                unsupportedTransaction("overwrite transaction not supported yet");
            } else {
                initTransaction(module);
                if (transactionDestDir.exists()) {
                    unsupportedTransaction("transaction destination directory already exists: "
                            + transactionDestDir
                            + "\npossible cause: usage of identifying tokens after the revision token");
                    closeTransaction();
                } else {
                    Message.verbose("\tstarting transaction: publish during transaction will be done in \n\t\t"
                            + transactionTempDir
                            + "\n\tand on commit moved to \n\t\t"
                            + transactionDestDir);
                }
            }
        }
    }

    @Override
    protected Collection<String> filterNames(Collection<String> values) {
        if (supportTransaction()) {
            values = super.filterNames(values);
            for (Iterator<String> iterator = values.iterator(); iterator.hasNext();) {
                String v = iterator.next();
                if (v.endsWith(TRANSACTION_DESTINATION_SUFFIX)) {
                    iterator.remove();
                }
            }
            return values;
        } else {
            return super.filterNames(values);
        }
    }

    public boolean supportTransaction() {
        if ("false".equals(transactional)) {
            return false;
        }
        checkSupportTransaction();
        return supportTransaction.booleanValue();
    }

    private void closeTransaction() {
        transactionTempDir = null;
        transactionDestDir = null;
    }

    private void checkSupportTransaction() {
        if (supportTransaction == null) {
            supportTransaction = Boolean.FALSE;
            List<String> ivyPatterns = getIvyPatterns();
            List<String> artifactPatterns = getArtifactPatterns();

            if (ivyPatterns.size() > 0) {
                String pattern = ivyPatterns.get(0);
                Matcher m = TRANSACTION_PATTERN.matcher(pattern);
                if (!m.matches()) {
                    unsupportedTransaction("ivy pattern does not use revision as a directory");
                    return;
                } else {
                    baseTransactionPattern = m.group(1);
                    fullTransactionPatterns.put(pattern, m.group(1)
                            + TRANSACTION_DESTINATION_SUFFIX + m.group(2));
                }
            }
            if (artifactPatterns.size() > 0) {
                String pattern = artifactPatterns.get(0);
                Matcher m = TRANSACTION_PATTERN.matcher(pattern);
                if (!m.matches()) {
                    unsupportedTransaction("artifact pattern does not use revision as a directory");
                    return;
                } else if (baseTransactionPattern != null) {
                    if (!baseTransactionPattern.equals(m.group(1))) {
                        unsupportedTransaction("ivy pattern and artifact pattern "
                                + "do not use the same directory for revision");
                        return;
                    } else {
                        fullTransactionPatterns.put(pattern, m.group(1)
                                + TRANSACTION_DESTINATION_SUFFIX + m.group(2));
                    }
                } else {
                    baseTransactionPattern = m.group(1);
                    fullTransactionPatterns.put(pattern, m.group(1)
                            + TRANSACTION_DESTINATION_SUFFIX + m.group(2));
                }
            }
            supportTransaction = Boolean.TRUE;
        }
    }

    private void unsupportedTransaction(String msg) {
        String fullMsg = getName() + " do not support transaction. " + msg;
        if ("true".equals(transactional)) {
            throw new IllegalStateException(fullMsg
                    + ". Set transactional attribute to 'auto' or 'false' or fix the problem.");
        } else {
            Message.verbose(fullMsg);
            supportTransaction = Boolean.FALSE;
        }
    }

    private void initTransaction(ModuleRevisionId module) {
        ModuleRevisionId mrid = module;
        if (isM2compatible()) {
            mrid = convertM2IdForResourceSearch(module);
        }

        transactionTempDir = Checks.checkAbsolute(
            IvyPatternHelper.substitute(
                baseTransactionPattern,
                ModuleRevisionId.newInstance(mrid, mrid.getRevision()
                        + TRANSACTION_DESTINATION_SUFFIX)), "baseTransactionPattern");
        transactionDestDir = Checks.checkAbsolute(
            IvyPatternHelper.substitute(baseTransactionPattern, mrid), "baseTransactionPattern");
    }

    public String getTransactional() {
        return transactional;
    }

    public void setTransactional(String transactional) {
        this.transactional = transactional;
    }

    @Override
    public void addConfiguredIvy(IvyPattern p) {
        File file = Checks.checkAbsolute(p.getPattern(), "ivy pattern");
        p.setPattern(file.getAbsolutePath());
        super.addConfiguredIvy(p);
    }

    @Override
    public void addIvyPattern(String pattern) {
        File file = Checks.checkAbsolute(pattern, "ivy pattern");
        super.addIvyPattern(file.getAbsolutePath());
    }

    @Override
    public void addConfiguredArtifact(IvyPattern p) {
        File file = Checks.checkAbsolute(p.getPattern(), "artifact pattern");
        p.setPattern(file.getAbsolutePath());
        super.addConfiguredArtifact(p);
    }

    @Override
    public void addArtifactPattern(String pattern) {
        File file = Checks.checkAbsolute(pattern, "artifact pattern");
        super.addArtifactPattern(file.getAbsolutePath());
    }
}
