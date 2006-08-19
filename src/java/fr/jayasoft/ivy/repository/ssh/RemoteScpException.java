/**
 * @author ace
 * $Id:$
 */
package fr.jayasoft.ivy.repository.ssh;

/**
 * This exception will be used for Remote SCP Exceptions (failures on the target system, no connetion probs) 
 */
public class RemoteScpException extends Exception {

    private static final long serialVersionUID = 3107198655563736600L;

    public RemoteScpException() {}

    /**
     * @param message
     */
    public RemoteScpException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public RemoteScpException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public RemoteScpException(String message, Throwable cause) {
        super(message, cause);
    }

}
