package fr.jayasoft.ivy.tools.analyser;

import java.io.File;

import fr.jayasoft.ivy.ModuleRevisionId;

public class JarModule {
	private ModuleRevisionId _mrid;
	private File _jar;
	
	public JarModule(ModuleRevisionId mrid, File jar) {
		_mrid = mrid;
		_jar = jar;
	}

	public File getJar() {
		return _jar;
	}

	public ModuleRevisionId getMrid() {
		return _mrid;
	}
	
	public String toString() {
		return _jar + " " + _mrid;
	}
	
}
