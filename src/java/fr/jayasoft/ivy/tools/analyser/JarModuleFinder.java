package fr.jayasoft.ivy.tools.analyser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.resolver.FileURLLister;
import fr.jayasoft.ivy.resolver.ResolverHelper;
import fr.jayasoft.ivy.resolver.URLLister;
import fr.jayasoft.ivy.util.IvyPatternHelper;

public class JarModuleFinder {
	private String _pattern;
	private String _filePattern;
	
	public JarModuleFinder(String pattern) {
		_pattern = "file:///"+pattern;
		_filePattern = pattern;
	}

	public JarModule[] findJarModules() {
		List ret = new ArrayList();
		URLLister lister = new FileURLLister();
		try {
			String[] orgs = ResolverHelper.listTokenValues(lister, _pattern, "organisation");
			for (int i = 0; i < orgs.length; i++) {
				String orgPattern = IvyPatternHelper.substituteToken(_pattern, IvyPatternHelper.ORGANISATION_KEY, orgs[i]);
				String[] modules = ResolverHelper.listTokenValues(lister, orgPattern, "module");
				for (int j = 0; j < modules.length; j++) {
					String modPattern = IvyPatternHelper.substituteToken(orgPattern, IvyPatternHelper.MODULE_KEY, modules[j]);
					String [] revs = ResolverHelper.listTokenValues(lister, modPattern, "revision");
					for (int k = 0; k < revs.length; k++) {
						File jar = new File(IvyPatternHelper.substitute(_filePattern, orgs[i], modules[j], revs[k], modules[j], "jar", "jar"));
						if (jar.exists()) {
							ret.add(new JarModule(ModuleRevisionId.newInstance(orgs[i], modules[j], revs[k]), jar));
						}
					}
				}
			}

		} catch (Exception e) {
			// TODO: handle exception
		}		
		return (JarModule[]) ret.toArray(new JarModule[ret.size()]);
	}

	public static void main(String[] args) {
		JarModule[] mods = new JarModuleFinder("D:/temp/test2/ivyrep/[organisation]/[module]/[revision]/[artifact].[ext]").findJarModules();
		for (int i = 0; i < mods.length; i++) {
			System.out.println(mods[i]);
		}
	}
}
