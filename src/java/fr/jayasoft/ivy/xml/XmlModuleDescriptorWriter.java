/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.Configuration;
import fr.jayasoft.ivy.DefaultModuleDescriptor;
import fr.jayasoft.ivy.DependencyArtifactDescriptor;
import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;

/**
 * @author Xavier Hanin
 *
 */
public class XmlModuleDescriptorWriter {
    public static void write(ModuleDescriptor md, File output) throws IOException {
        if (output.getParentFile() != null) {
            output.getParentFile().mkdirs();
        }
        PrintWriter out = new PrintWriter(new FileOutputStream(output));
        try {
	        out.println("<ivy-module version=\"1.0\">"); 
	    	out.println("\t<info organisation=\""+md.getModuleRevisionId().getOrganisation()+"\"");
	    	out.println("\t\tmodule=\""+md.getModuleRevisionId().getName()+"\"");
	    	String branch = md.getResolvedModuleRevisionId().getBranch();
            if (branch != null) {
                out.println("\t\tbranch=\""+branch+"\"");
            }
	    	String revision = md.getResolvedModuleRevisionId().getRevision();
            if (revision != null) {
                out.println("\t\trevision=\""+revision+"\"");
            }
	    	out.println("\t\tstatus=\""+md.getStatus()+"\"");
	    	out.println("\t\tpublication=\""+Ivy.DATE_FORMAT.format(md.getResolvedPublicationDate())+"\"");
            if (md.isDefault()) {
                out.println("\t\tdefault=\"true\"");
            }
            if (md instanceof DefaultModuleDescriptor) {
                DefaultModuleDescriptor dmd = (DefaultModuleDescriptor)md;
                if (dmd.getNamespace() != null && !dmd.getNamespace().getName().equals("system")) {
                    out.println("\t\tnamespace=\""+dmd.getNamespace().getName()+"\"");
                }
            }
	    	out.println("\t/>");
            Configuration[] confs = md.getConfigurations();
            if (confs.length > 0) {
                out.println("\t<configurations>");
                for (int i = 0; i < confs.length; i++) {
                    out.print("\t\t<conf");
                    out.print(" name=\""+confs[i].getName()+"\"");
                    out.print(" visibility=\""+confs[i].getVisibility()+"\"");
                    if (confs[i].getDescription() != null) {
                        out.print(" description=\""+confs[i].getDescription()+"\"");
                    }
                    String[] exts = confs[i].getExtends();
                    if (exts.length > 0) {
                        out.print(" extends=\"");
                        for (int j = 0; j < exts.length; j++) {
                            out.print(exts[j]);
                            if (j+1 < exts.length) {
                                out.print(",");
                            }
                        }
                        out.print("\"");
                    }
                    out.println("/>");
                }
                out.println("\t</configurations>");
            }
            out.println("\t<publications>");
            Artifact[] artifacts = md.getAllArtifacts();
            for (int i = 0; i < artifacts.length; i++) {
                out.print("\t\t<artifact");
                out.print(" name=\""+artifacts[i].getName()+"\"");
                out.print(" type=\""+artifacts[i].getType()+"\"");
                out.print(" ext=\""+artifacts[i].getExt()+"\"");
                out.print(" conf=\""+getConfs(md, artifacts[i])+"\"");
                out.println("/>");
            }
            out.println("\t</publications>");
            
            DependencyDescriptor[] dds = md.getDependencies();
            if (dds.length > 0) {
                out.println("\t<dependencies>");
                for (int i = 0; i < dds.length; i++) {
                    out.print("\t\t<dependency");
                    out.print(" org=\""+dds[i].getDependencyRevisionId().getOrganisation()+"\"");
                    out.print(" name=\""+dds[i].getDependencyRevisionId().getName()+"\"");
                    out.print(" rev=\""+dds[i].getDependencyRevisionId().getRevision()+"\"");
                    if (dds[i].isForce()) {
                    	out.print(" force=\""+dds[i].isForce()+"\"");
                    }
                    if (dds[i].isChanging()) {
                    	out.print(" changing=\""+dds[i].isChanging()+"\"");
                    }
                    if (!dds[i].isTransitive()) {
                    	out.print(" transitive=\""+dds[i].isTransitive()+"\"");
                    }
                    out.print(" conf=\"");
                    String[] modConfs = dds[i].getModuleConfigurations();
                    for (int j = 0; j < modConfs.length; j++) {
                        String[] depConfs = dds[i].getDependencyConfigurations(modConfs[j]);
                        out.print(modConfs[j]+"->");
                        for (int k = 0; k < depConfs.length; k++) {
                            out.print(depConfs[k]);
                            if (k+1 < depConfs.length) {
                                out.print(",");
                            }
                        }
                        if (j+1 < modConfs.length) {
                            out.print(";");
                        }
                    }
                    out.print("\"");
                    DependencyArtifactDescriptor[] includes = dds[i].getAllDependencyArtifactsIncludes();
                    if (includes.length > 0) {
                        out.println(">");
                        for (int j = 0; j < includes.length; j++) {
                            out.print("\t\t\t<include");
                            out.print(" name=\""+includes[j].getName()+"\"");
                            out.print(" type=\""+includes[j].getType()+"\"");
                            out.print(" ext=\""+includes[j].getExt()+"\"");
                            String[] dadconfs = includes[j].getConfigurations();
                            if (!Arrays.asList(dadconfs).equals(Arrays.asList(md.getConfigurationsNames()))) {
                                out.print(" conf=\"");
                                for (int k = 0; k < dadconfs.length; k++) {
                                    out.print(dadconfs[k]);
                                    if (k+1 < dadconfs.length) {
                                        out.print(",");
                                    }
                                }
                                out.print("\"");
                            }
                            out.println("/>");
                        }
                    }
                    DependencyArtifactDescriptor[] excludes = dds[i].getAllDependencyArtifactsExcludes();
                    if (excludes.length > 0) {
                        if (includes.length == 0) {
                            out.println(">");
                        }
                        for (int j = 0; j < excludes.length; j++) {
                            out.print("\t\t\t<exclude");
                            out.print(" org=\""+excludes[j].getId().getModuleId().getOrganisation()+"\"");
                            out.print(" module=\""+excludes[j].getId().getModuleId().getName()+"\"");
                            out.print(" name=\""+excludes[j].getName()+"\"");
                            out.print(" type=\""+excludes[j].getType()+"\"");
                            out.print(" ext=\""+excludes[j].getExt()+"\"");
                            String[] dadconfs = excludes[j].getConfigurations();
                            if (!Arrays.asList(dadconfs).equals(Arrays.asList(md.getConfigurationsNames()))) {
                                out.print(" conf=\"");
                                for (int k = 0; k < dadconfs.length; k++) {
                                    out.print(dadconfs[k]);
                                    if (k+1 < dadconfs.length) {
                                        out.print(",");
                                    }
                                }
                                out.print("\"");
                            }
                            out.println("/>");
                        }
                    }
                    if (includes.length + excludes.length == 0) {
                        out.println("/>");
                    } else {
                        out.println("\t\t</dependency>");
                    }
                }
                out.println("\t</dependencies>");
            }
            out.println("</ivy-module>");
        } finally {
            out.close();
        }
    }

    private static String getConfs(ModuleDescriptor md, Artifact artifact) {
        StringBuffer ret = new StringBuffer();
    
        String[] confs = md.getConfigurationsNames();
        for (int i = 0; i < confs.length; i++) {
            if (Arrays.asList(md.getArtifacts(confs[i])).contains(artifact)) {
                ret.append(confs[i]).append(",");
            }
        }
        if (ret.length() > 0) {
            ret.setLength(ret.length() - 1);
        }
        return ret.toString();
    }
}
