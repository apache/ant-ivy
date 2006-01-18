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

import fr.jayasoft.ivy.DependencyDescriptor;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;

/**
 * @author Xavier Hanin
 *
 */
public class XmlModuleDescriptorWriter {
    // only handle header and info for the moment
    public static void write(ModuleDescriptor md, File output) throws IOException {
        if (output.getParentFile() != null) {
            output.getParentFile().mkdirs();
        }
        PrintWriter out = new PrintWriter(new FileOutputStream(output));
        try {
	        out.println("<ivy-module version=\"1.0\">"); 
	    	out.println("\t<info organisation=\""+md.getModuleRevisionId().getOrganisation()+"\"");
	    	out.println("\t\tmodule=\""+md.getModuleRevisionId().getName()+"\"");
	    	out.println("\t\trevision=\""+md.getResolvedModuleRevisionId().getRevision()+"\"");
	    	out.println("\t\tstatus=\""+md.getStatus()+"\"");
	    	out.println("\t\tpublication=\""+Ivy.DATE_FORMAT.format(md.getResolvedPublicationDate())+"\"");
            out.println("\t\tdefault=\""+(md.isDefault()?"true":"false")+"\"");
	    	out.println("\t/>");
            DependencyDescriptor[] dds = md.getDependencies();
            if (dds.length > 0) {
                out.println("\t<dependencies>");
                for (int i = 0; i < dds.length; i++) {
                    out.print("\t\t<dependency");
                    out.print(" org=\""+dds[i].getDependencyRevisionId().getOrganisation()+"\"");
                    out.print(" name=\""+dds[i].getDependencyRevisionId().getName()+"\"");
                    out.print(" rev=\""+dds[i].getDependencyRevisionId().getRevision()+"\"");
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
                    out.println("\"/>");
                }
                out.println("\t</dependencies>");
            }
            out.println("</ivy-module>");
        } finally {
            out.close();
        }
    }
}
