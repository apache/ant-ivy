/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

/**
 * This class contains information about the origin of an artifact.
 * 
 * @author maartenc
 */
public class ArtifactOrigin {
	private boolean _isLocal;
	private String _location;
	public ArtifactOrigin(boolean isLocal, String location) {
		_isLocal = isLocal;
		_location = location;
	}
	public boolean isLocal() {
		return _isLocal;
	}
	public String getLocation() {
		return _location;
	}
}
