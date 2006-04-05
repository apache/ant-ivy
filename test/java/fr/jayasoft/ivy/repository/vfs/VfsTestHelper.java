package fr.jayasoft.ivy.repository.vfs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs.impl.StandardFileSystemManager;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.util.FileUtil;

public class VfsTestHelper {
	private Ivy ivy = null;
	public StandardFileSystemManager fsManager = null;
	final static public String VFS_CONF = "ivy_vfs.xml";
	
	// Ivy Variables
	static final public String PROP_VFS_HOST = "vfs.host";
	static final public String PROP_VFS_SAMBA_REPO = "vfs.samba.share";
	static final public String PROP_VFS_USER_ID = "vfs.user";
	static final public String PROP_VFS_USER_PASSWD = "vfs.passwd";
	
	// Resources
	static final public String CWD = System.getProperty("user.dir");
	static final public String TEST_REPO_DIR = "test/repositories";
	static final private String IVY_CONFIG_FILE = 
		FileUtil.concat(TEST_REPO_DIR, "ivyconf.xml");
	static final public String TEST_IVY_XML = "2/mod5.1/ivy-4.2.xml";
	static final public String SCRATCH_DIR = "_vfsScratchArea";

	
	public VfsTestHelper() throws Exception {
		// setup and initialize VFS
		fsManager = new StandardFileSystemManager();
		fsManager.setConfiguration(getClass().getResource(VFS_CONF).toString());
		fsManager.init();
		
		// setup and initialize ivy
		ivy = new Ivy();
		ivy.configure(new File(IVY_CONFIG_FILE));
	}

	/**
	 * Generate a set of well-formed VFS resource identifiers
	 * 
	 * @param resource name of the resource
	 * @return <class>List</class> of well-formed VFS reosurce identifiers
	 */
	public List createVFSUriSet(String resource) {
		List set = new ArrayList();
		for (int i = 0; i < VfsURI.SUPPORTED_SCHEMES.length; i++) {
			set.add(VfsURI.vfsURIFactory(VfsURI.SUPPORTED_SCHEMES[i], 
					                        resource,
					                        ivy));			
		}	
		return set;
	}
	
	
	public Ivy getIvy() {
		return ivy;
	}
	

}
