package fr.jayasoft.ivy.tools.analyser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import fr.jayasoft.ivy.DefaultDependencyDescriptor;
import fr.jayasoft.ivy.DefaultModuleDescriptor;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.util.Message;

public class JarJarDependencyAnalyser implements DependencyAnalyser {
	private File _jarjarjarLocation;
	
	public JarJarDependencyAnalyser(File jarjarjarLocation) {
		_jarjarjarLocation = jarjarjarLocation;
	}


	public ModuleDescriptor[] analyze(JarModule[] modules) {
		
		StringBuffer jarjarCmd = new StringBuffer("java -jar \"").append(_jarjarjarLocation.getAbsolutePath()).append("\" --find --level=jar ");
		Map jarModulesMap = new HashMap();
		Map mds = new HashMap();

		for (int i = 0; i < modules.length; i++) {
			jarModulesMap.put(modules[i].getJar().getAbsolutePath(), modules[i]);
			DefaultModuleDescriptor md = DefaultModuleDescriptor.newBasicInstance(modules[i].getMrid(), new Date(modules[i].getJar().lastModified()));
			mds.put(modules[i].getMrid(), md);
			jarjarCmd.append("\"").append(modules[i].getJar().getAbsolutePath()).append("\"");
			if (i+1 < modules.length) {
				jarjarCmd.append(File.pathSeparator);
			}
		}
		
		Message.verbose("jarjar command: "+jarjarCmd);
		
		try {
			Process p = Runtime.getRuntime().exec(jarjarCmd.toString());
			BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = r.readLine()) != null) {
				String[] deps = line.split(" -> ");
				JarModule module = (JarModule) jarModulesMap.get(deps[0]);
				JarModule dependency = (JarModule) jarModulesMap.get(deps[1]);
				
				if (module.getMrid().getModuleId().equals(dependency.getMrid().getModuleId())) {
					continue;
				}
				Message.verbose(module.getMrid() + " depends on "  + dependency.getMrid());

				DefaultModuleDescriptor md = (DefaultModuleDescriptor) mds.get(module.getMrid());
				
				DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(dependency.getMrid(), false);
				dd.addDependencyConfiguration(ModuleDescriptor.DEFAULT_CONFIGURATION, ModuleDescriptor.DEFAULT_CONFIGURATION);
				md.addDependency(dd);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return (ModuleDescriptor[]) mds.values().toArray(new ModuleDescriptor[mds.values().size()]);
	}

	
	public static void main(String[] args) {
		JarJarDependencyAnalyser a = new JarJarDependencyAnalyser(new File("D:/temp/test2/jarjar-0.7.jar"));
		a.analyze(new JarModuleFinder("D:/temp/test2/ivyrep/[organisation]/[module]/[revision]/[artifact].[ext]").findJarModules());
	}
}
