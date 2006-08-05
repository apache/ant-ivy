package fr.jayasoft.ivy.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.DirSet;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Path.PathElement;

/**
 * This task is not directly related to ivy, but is useful in some modular build systems.
 * 
 * The idea is to be able to contribute new sub path elements to an existing path.
 * 
 * @author Xavier Hanin
 */
public class AddPathTask extends Task {
	private String _topath;
	private boolean _first = false;
	private Path _toadd;

	public String getTopath() {
		return _topath;
	}

	public void setTopath(String topath) {
		_topath = topath;
	}
	
	public void setProject(Project project) {
		super.setProject(project);
		_toadd = new Path(project);
	}
	
    
	public void execute() throws BuildException {
		Object element = getProject().getReference(_topath);
		if (element == null) {
			throw new BuildException("destination path not found: "+_topath);
		}
		if (! (element instanceof Path)) {
			throw new BuildException("destination path is not a path: "+element.getClass());
		}
		Path dest = (Path) element;
		if (_first) {
			// now way to add path elements at te beginning of the existing path: we do the opposite
			// and replace the reference
			_toadd.append(dest);
			getProject().addReference(_topath, _toadd);
		} else {
			dest.append(_toadd);
		}
	}

	public void add(Path path) throws BuildException {
		_toadd.add(path);
	}

	public void addDirset(DirSet dset) throws BuildException {
		_toadd.addDirset(dset);
	}

	public void addFilelist(FileList fl) throws BuildException {
		_toadd.addFilelist(fl);
	}

	public void addFileset(FileSet fs) throws BuildException {
		_toadd.addFileset(fs);
	}

	public Path createPath() throws BuildException {
		return _toadd.createPath();
	}

	public PathElement createPathElement() throws BuildException {
		return _toadd.createPathElement();
	}

	public boolean isFirst() {
		return _first;
	}

	public void setFirst(boolean first) {
		_first = first;
	}
}
