package fr.jayasoft.ivy.tools.analyser;

import java.io.File;
import java.io.IOException;

import fr.jayasoft.ivy.DefaultArtifact;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.util.IvyPatternHelper;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.xml.XmlModuleDescriptorWriter;

public class RepositoryAnalyser {
	public void analyse(String pattern, DependencyAnalyser depAnalyser) {
		JarModuleFinder finder = new JarModuleFinder(pattern);
		ModuleDescriptor[] mds = depAnalyser.analyze(finder.findJarModules());
		Message.info("found "+mds.length+" modules");
		for (int i = 0; i < mds.length; i++) {
			File ivyFile = new File(IvyPatternHelper.substitute(pattern, DefaultArtifact.newIvyArtifact(mds[i].getModuleRevisionId(), mds[i].getPublicationDate())));
			try {
				Message.info("generating "+ivyFile);
				XmlModuleDescriptorWriter.write(mds[i], ivyFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("usage: ivyanalyser path/to/jarjar.jar absolute-ivy-repository-pattern");
			return;
		}
		String jarjarLocation = args[0];
		String pattern = args[1];

		JarJarDependencyAnalyser a = new JarJarDependencyAnalyser(new File(jarjarLocation));
		new RepositoryAnalyser().analyse(pattern, a);
	}
}
