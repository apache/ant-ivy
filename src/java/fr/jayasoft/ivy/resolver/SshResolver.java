package fr.jayasoft.ivy.resolver;

import fr.jayasoft.ivy.repository.ssh.SshRepository;

/**
 * Resolver for SSH resolver for ivy
 */
public class SshResolver extends AbstractSshBasedResolver {
	
    public SshResolver() {
        setRepository(new SshRepository());
    }
    /**
     * sets the path separator used on the target system. Not sure if this is used or if '/' is used 
     * on all implementation. default is to use '/'
     * @param sep file separator to use on the target system
     */
    public void setFileSeparator(String sep) {
        if(sep == null || sep.length() != 1) 
            throw new IllegalArgumentException("File Separator has to be a single character and not "+sep);
        ((SshRepository)getRepository()).setFileSeparator(sep.trim().charAt(0));
    }
    
    /**
     * set the command to get a directory listing
     * the command has to be a shell command working on the target system 
     * and has to produce a listing of filenames, with each filename on a new line
     * the term %arg can be used in the command to substitue the path to be listed
     * (e.g. "ls -1 %arg | grep -v CVS" to get a listing without CVS directory)
     * if %arg is not part of the command, the path will be appended to the command
     * default is: "ls -1"
     */
    public void setListCommand(String cmd) {
        ((SshRepository)getRepository()).setListCommand(cmd);
    }

    /**
     * set the command to check for existence of a file
     * the command has to be a shell command working on the target system 
     * and has to create an exit status of 0 for an existent file
     * and <> 0 for a non existing file given as argument 
     * the term %arg can be used in the command to substitue the path to be listed
     * if %arg is not part of the command, the path will be appended to the command
     * default is: "ls"
     */
    public void setExistCommand(String cmd) {
        ((SshRepository)getRepository()).setExistCommand(cmd);
    }

    /**
     * set the command to create a directory on the target system
     * the command has to be a shell command working on the target system 
     * and has to create a directory with the given argument
     * the term %arg can be used in the command to substitue the path to be listed
     * if %arg is not part of the command, the path will be appended to the command
     * default is: "mkdir"
     */
    public void setCreateDirCommand(String cmd) {
        ((SshRepository)getRepository()).setExistCommand(cmd);
    }

    public String getTypeName() {
        return "ssh";
    }
}
