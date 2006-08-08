package fr.jayasoft.ivy.tools.analyser;

import fr.jayasoft.ivy.ModuleDescriptor;

public interface DependencyAnalyser {
	public ModuleDescriptor[] analyze(JarModule[] modules);
}
