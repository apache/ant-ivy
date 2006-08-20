package fr.jayasoft.ivy.repository.ssh;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.jcraft.jsch.Session;

import fr.jayasoft.ivy.repository.AbstractRepository;
import fr.jayasoft.ivy.util.Message;

public abstract class AbstractSshBasedRepository extends AbstractRepository {

    private File keyFile = null;
    private File passFile = null;
    private String userPassword = null;
    private String keyFilePassword = null;
    private String user = "guest";
    private String host = null;
    private int port = -1;

    public AbstractSshBasedRepository() {
        super();
    }

    /**
     * get a new session using the default attributes
     * if the given String is a full uri, use the data from the uri
     * instead
     * @param pathOrUri might be just a path or a full ssh or sftp uri
     * @return matching Session
     */
    protected Session getSession(String pathOrUri) throws IOException {
        URI uri = parseURI(pathOrUri);
        String host = getHost();
        int port = getPort();
        String user = getUser();
        String userPassword = getUserPassword();
        if(uri != null && uri.getScheme() != null) {
            if(uri.getHost() != null )
                host = uri.getHost();
            if(uri.getPort() != -1) {
                port = uri.getPort();
            }
            if(uri.getUserInfo() != null) {
                String userInfo = uri.getUserInfo();
                if(userInfo.indexOf(":") == -1) {
                    user = userInfo;
                } else {
                    user = userInfo.substring(0, userInfo.indexOf(":"));
                    userPassword = userInfo.substring(userInfo.indexOf(":")+1);
                }
            }
        }
        return SshCache.getInstance().getSession(host, 
                                                 port,
                                                 user,
                                                 userPassword, 
                                                 getKeyFile(),
                                                 getKeyFilePassword(),
                                                 getPassFile());
    }
        
    /**
     * Just check the uri for sanity
     * @param source String of the uri
     * @return URI object of the String or null
     */
    private URI parseURI(String source) {
        try {
            URI uri = new URI(source);
            if(uri.getScheme() != null && !uri.getScheme().equalsIgnoreCase(getRepositoryScheme()))
                throw new URISyntaxException(source,"Wrong scheme in URI. Expected "+getRepositoryScheme()+" as scheme!");
            if(uri.getHost() == null && getHost() == null)
                throw new URISyntaxException(source,"Missing host in URI or in resolver");
            if(uri.getPath() == null)
                throw new URISyntaxException(source,"Missing path in URI");
            if(uri.getUserInfo() == null && getUser() == null)
                throw new URISyntaxException(source,"Missing username in URI or in resolver");
            return uri;
        } catch (URISyntaxException e) {
            Message.error(e.getMessage());
            Message.error("The uri is in the wrong format.");
            Message.error("Please use scheme://user:pass@hostname/path/to/repository");
            return null;
        }
    }

    /**
     * closes the session and remove it from the cache (eg. on case of errors)
     * @param uri key for the cache
     * @param conn to release
     */
    protected void releaseSession(Session session,String pathOrUri) {
        session.disconnect();
        SshCache.getInstance().clearSession(session);
    }

    /**
     * set the default user to use for the connection if no user is given or a PEM file is used
     * @param user to use
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the user to use for the connection if no user is given or a PEM file is used
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the full file path to use for accessing a PEM key file 
     * @param filePath fully qualified name
     */
    public void setKeyFile(File filePath) {
        this.keyFile = filePath;
        if(!keyFile.exists()) {
            Message.warn("Pemfile "+keyFile.getAbsolutePath()+" doesn't exist.");
            keyFile = null;
        } else if(!keyFile.canRead()) {
            Message.warn("Pemfile "+keyFile.getAbsolutePath()+" not readable.");
            keyFile = null;
        } else {
            Message.debug("Using "+keyFile.getAbsolutePath()+" as keyfile.");
        }
    }

    /**
     * @return the keyFile
     */
    public File getKeyFile() {
        return keyFile;
    }

    /**
     * @param user password to use for user/password authentication
     */
    public void setUserPassword(String password) {
        this.userPassword = password;
    }

    /**
     * @return the keyFile password for public key based authentication
     */
    public String getKeyFilePassword() {
        return keyFilePassword;
    }
    
    /**
     * @param keyFilePassword sets password for public key based authentication
     */
    public void setKeyFilePassword(String keyFilePassword) {
        this.keyFilePassword = keyFilePassword;
    }
    /**
     * @return the user password
     */
    public String getUserPassword() {
        return userPassword;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }
    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }
    
    /**
     * @param passFile the passfile to set
     */
    public void setPassFile(File passFile) {
        this.passFile = passFile;
    }
    
    /**
     * @return the passFile 
     */
    public File getPassFile() {
        return passFile;
    }

    protected abstract String getRepositoryScheme();

}