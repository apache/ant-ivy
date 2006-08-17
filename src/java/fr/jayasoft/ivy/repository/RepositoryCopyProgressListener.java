/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.repository;

import fr.jayasoft.ivy.util.CopyProgressEvent;
import fr.jayasoft.ivy.util.CopyProgressListener;

public class RepositoryCopyProgressListener implements CopyProgressListener {
    private final AbstractRepository _repository;

    public RepositoryCopyProgressListener(AbstractRepository repository) {
        _repository = repository;
    }

    private Long _totalLength = null;
    public void start(CopyProgressEvent evt) {
        if (_totalLength != null) {
            _repository.fireTransferStarted(_totalLength.longValue());
        } else {
            _repository.fireTransferStarted();
        }
    }

    public void progress(CopyProgressEvent evt) {
        _repository.fireTransferProgress(evt.getReadBytes());
    }

    public void end(CopyProgressEvent evt) {
        _repository.fireTransferProgress(evt.getReadBytes());
        _repository.fireTransferCompleted();
    }

    public Long getTotalLength() {
        return _totalLength;
    }

    public void setTotalLength(Long totalLength) {
        _totalLength = totalLength;
    }
}