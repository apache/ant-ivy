package fr.jayasoft.ivy.resolver;

import java.io.File;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.repository.ssh.AbstractSshBasedRepository;

/**
 * Abstract base class for all resolvers using SSH 
 * 
 * All necessary connection parameters can be set here via attributes.
 * However all attributes defined in the pattern url of the resolver will have higher 
 * priority and will overwrite the values given here. To specify connection parameters
 * in the pattern, you have to specify a full url and not just a path as pattern.
 * e.g. pattern="/path/to/my/repos/[artifact].[ext]" will use all connection parameters 
 * from this class
 * e.g. pattern="ssh://myserver.com/path/to/my/repos/[artifact].[ext]" will use all parameters 
 * from this class with the exception of the host, which will be "myserver.com"
 * e.g. pattern="sftp://user:geheim@myserver.com:8022/path/to/my/repos/[artifact].[ext]" will
 * use only the keyFile and keyFilePassword from this class (if needed). Rest will come from the url.
 */
public abstract class AbstractSshBasedResolver extends RepositoryResolver {

    private boolean passfileSet = false;

    public AbstractSshBasedResolver() {
        super();
    }

    private AbstractSshBasedRepository getSshBasedRepository() {
		return ((AbstractSshBasedRepository)getRepository());
	}

    /**
     * Sets the location of the Public Key file to use for authentication
     * @param filePath full file path name
     */
    public void setKeyFile(String filePath) {
    	getSshBasedRepository().setKeyFile(new File(filePath));
    }

    /** 
     * Optional password file. If set the repository will use it as an encypted property file, to load
     * username and passwd entries, and to store them if the user choose to do so.
     * 
     * Defaults to user.dir/.ivy/[host].sftp.passwd, set it to null to disable this feature. 
     */
    public void setPassfile(String passfile) {
        getSshBasedRepository().setPassFile(passfile == null ? null : new File(passfile));
        passfileSet = true;
    }

    public void setIvy(Ivy ivy) {
        super.setIvy(ivy);
        if (!passfileSet) {
            getSshBasedRepository().setPassFile(new File(ivy.getDefaultIvyUserDir(), getSshBasedRepository().getHost()+".ssh.passwd"));
        }
    }

    /**
     * Sets the password to authenticate the user if password based login is used
     * if no password is set and password based login is used, user will be prompted for it
     * the password can also be set by using a full url for the pattern, like
     * "sftp://user:password@myserver.com/path/to/repos/[artifact].[ext]"
     * @param password to use
     */
    public void setUserPassword(String password) {
    	getSshBasedRepository().setUserPassword(password);
    }

    /**
     * Sets the password to use for decrypting key file (if it is encrypted)
     * if no password is set and the keyfile is encrypted, the user will be prompted for the password
     * if the keyfile is passwordless, this parameter will be ignored if given
     * @param password to use
     */
    public void setKeyFilePassword(String password) {
        getSshBasedRepository().setKeyFilePassword(password);
    }

    /**
     * sets the user to use for the ssh communication
     * the user can also be set by using a full url for the pattern, like
     * "ssh://user@myserver.com/path/to/repos/[artifact].[ext]"
     * @param user on the target system
     */
    public void setUser(String user) {
        getSshBasedRepository().setUser(user);
    }

    /**
     * sets the host to use for the ssh communication
     * the host can also be set by using a full url for the pattern, like
     * "ssh://myserver.com/path/to/repos/[artifact].[ext]"
     * @param host of the target system
     */
    public void setHost(String host) {
        getSshBasedRepository().setHost(host);
    }

    /**
     * sets the port to use for the ssh communication
     * port 22 is default
     * the port can also be set by using a full url for the pattern, like
     * "sftp://myserver.com:8022/path/to/repos/[artifact].[ext]"
     * @param port of the target system
     */
    public void setPort(int port) {
        getSshBasedRepository().setPort(port);
    }
    
    abstract public String getTypeName();
}