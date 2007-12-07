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
import org.apache.ivy.plugins.repository.file.FileRepository;
import org.apache.ivy.util.Message;

/**
 */
public class FileSystemResolver extends RepositoryResolver {

    private static final String TRANSACTION_DESTINATION_SUFFIX = ".part";
    private static final Pattern TRANSACTION_PATTERN = 
        Pattern.compile("(.*\\[revision\\])([/\\\\][^/\\\\]+)");
    
    /**
     * Transactional mode.
     * 
     * auto: use transaction if possible, only log verbose message if not
     * true: always use transaction, fail if not supported
     * false: never use transactions
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
    private Map/*<String,String>*/ fullTransactionPatterns = new HashMap();

    /**
     * Is the current transaction open in overwrite mode?
     */
    private boolean overwriteTransaction = false;
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
    

    protected String getDestination(String pattern, Artifact artifact, ModuleRevisionId mrid) {
        if (supportTransaction() && !overwriteTransaction) {
            
            String destPattern = (String) fullTransactionPatterns.get(pattern);
            if (destPattern == null) {
                throw new IllegalArgumentException(
                    "unsupported pattern for publish destination pattern: " + pattern 
                    + ". supported patterns: " + fullTransactionPatterns.keySet());
            }
            return IvyPatternHelper.substitute(
                destPattern, 
                mrid, 
                artifact);
        } else {
            return super.getDestination(pattern, artifact, mrid);
        }
    }

    public void abortPublishTransaction() throws IOException {
        if (supportTransaction() && !overwriteTransaction) {
            if (transactionTempDir == null) {
                throw new IllegalStateException("no current transaction!");
            }
            getFileRepository().delete(transactionTempDir);
            Message.info("\tpublish aborted: deleted " + transactionTempDir);
            closeTransaction();
        }
    }

    public void commitPublishTransaction() throws IOException {
        if (supportTransaction() && !overwriteTransaction) {
            if (transactionTempDir == null) {
                throw new IllegalStateException("no current transaction!");
            }
            getFileRepository().move(transactionTempDir, transactionDestDir);
            Message.info("\tpublish commited: moved " + transactionTempDir 
                + " \n\t\tto " + transactionDestDir);
            closeTransaction();
        }
    }

    public void beginPublishTransaction(
            ModuleRevisionId module, boolean overwrite) throws IOException {
        if (supportTransaction()) {
            if (transactionTempDir != null) {
                throw new IllegalStateException("a transaction is already started and not closed!");
            }
            overwriteTransaction = overwrite;
            if (overwriteTransaction) {
                unsupportedTransaction("overwrite transaction not supported yet");
            } else {
                initTransaction(module);
            }
        }
    }
    
    protected Collection filterNames(Collection values) {
        if (supportTransaction()) {
            values =  super.filterNames(values);
            for (Iterator iterator = values.iterator(); iterator.hasNext();) {
                String v = (String) iterator.next();
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
            List ivyPatterns = getIvyPatterns();
            List artifactPatterns = getArtifactPatterns();
            
            if (ivyPatterns.size() > 0) {
                String pattern = (String) ivyPatterns.get(0);
                Matcher m = TRANSACTION_PATTERN.matcher(pattern);
                if (!m.matches()) {
                    unsupportedTransaction("ivy pattern does not use revision as last directory");
                    return;
                } else {
                    baseTransactionPattern = m.group(1);
                    fullTransactionPatterns.put(pattern, 
                        m.group(1) + TRANSACTION_DESTINATION_SUFFIX + m.group(2));
                }
            }
            if (artifactPatterns.size() > 0) {
                String pattern = (String) artifactPatterns.get(0);
                Matcher m = TRANSACTION_PATTERN.matcher(pattern);
                if (!m.matches()) {
                    unsupportedTransaction("ivy pattern does not use revision as last directory");
                    return;
                } else if (baseTransactionPattern != null) {
                    if (!baseTransactionPattern.equals(m.group(1))) {
                        unsupportedTransaction("ivy pattern and artifact pattern "
                            + "do not use the same directory for revision");
                        return;
                    } else {
                        fullTransactionPatterns.put(pattern, 
                            m.group(1) + TRANSACTION_DESTINATION_SUFFIX + m.group(2));
                    }
                } else {
                    baseTransactionPattern = m.group(1);
                    fullTransactionPatterns.put(pattern, 
                        m.group(1) + TRANSACTION_DESTINATION_SUFFIX + m.group(2));
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
        transactionTempDir = new File(IvyPatternHelper.substitute(
            baseTransactionPattern, 
            ModuleRevisionId.newInstance(
                module, module.getRevision() + TRANSACTION_DESTINATION_SUFFIX)));
        transactionDestDir = new File(IvyPatternHelper.substitute(
            baseTransactionPattern, 
            module));
    }

    public String getTransactional() {
        return transactional;
    }

    public void setTransactional(String transactional) {
        this.transactional = transactional;
    }

}
