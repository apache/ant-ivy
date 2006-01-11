/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.repository;

import java.util.EventListener;


/**
 * Listen to repository transfer
 */
public interface TransferListener extends EventListener {
    void transferProgress(TransferEvent evt);
}
