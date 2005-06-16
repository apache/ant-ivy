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
            out.println("\t\tresolver=\""+md.getResolverName()+"\"");
            out.println("\t\tdefault=\""+(md.isDefault()?"true":"false")+"\"");
	    	out.println("\t/>");
	    	out.println("</ivy-module>");
        } finally {
            out.close();
        }
    }
}
