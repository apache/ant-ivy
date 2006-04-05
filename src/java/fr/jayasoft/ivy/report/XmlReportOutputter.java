/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.IvyNode;
import fr.jayasoft.ivy.License;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleId;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.util.FileUtil;
import fr.jayasoft.ivy.util.Message;

/**
 * @author Xavier Hanin
 *
 */
public class XmlReportOutputter implements ReportOutputter {

    public void output(ResolveReport report, File destDir) {
        String[] confs = report.getConfigurations();
        for (int i = 0; i < confs.length; i++) {
            output(report.getConfigurationReport(confs[i]), destDir);
        }
    }
    
    public void output(ConfigurationResolveReport report, File destDir) {
    	PrintWriter out = null;
    	try {
    	    destDir.mkdirs();
    		File reportFile = new File(destDir, getReportFileName(report));
			out = new PrintWriter(new FileOutputStream(reportFile));
    		ModuleRevisionId mrid = report.getModuleDescriptor().getModuleRevisionId();
            out.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
            out.println("<?xml-stylesheet type=\"text/xsl\" href=\"ivy-report.xsl\"?>");
    		out.println("<ivy-report version=\"1.0\">");
    		out.println("\t<info");
    		out.println("\t\torganisation=\""+mrid.getOrganisation()+"\"");
    		out.println("\t\tmodule=\""+mrid.getName()+"\"");
    		out.println("\t\tconf=\""+report.getConfiguration()+"\"");
    		out.println("\t\tdate=\""+Ivy.DATE_FORMAT.format(report.getDate())+"\"/>");
    		
    		out.println("\t<dependencies>");
    		for (Iterator iter = report.getModuleIds().iterator(); iter.hasNext();) {
    			ModuleId mid = (ModuleId) iter.next();
				out.println("\t\t<module organisation=\""+mid.getOrganisation()+"\"" +
						" name=\""+mid.getName()+"\"" +
						" resolver=\""+report.getIvy().getResolverName(mid)+"\">");
				for (Iterator it2 = report.getNodes(mid).iterator(); it2.hasNext();) {
                    IvyNode dep = (IvyNode)it2.next();
                    ModuleDescriptor md = null;
                    if (dep.getModuleRevision() != null) {
                        md = dep.getModuleRevision().getDescriptor();
                    }
                    String details = "";
                    if  (dep.isLoaded()) {
                        details += " status=\""+dep.getDescriptor().getStatus()+"\"" +
                            " pubdate=\""+Ivy.DATE_FORMAT.format(new Date(dep.getPublication()))+"\"" +
                            " resolver=\""+dep.getModuleRevision().getResolver().getName()+"\""+
                            " artresolver=\""+dep.getModuleRevision().getArtifactResolver().getName()+"\"";
                    }
                    if (dep.isEvicted(report.getConfiguration())) {
                        IvyNode.EvictionData ed = dep.getEvictedData(report.getConfiguration());
                        if (ed.getConflictManager() != null) {
                            details += " evicted=\""+ed.getConflictManager()+"\"";
                        } else {
                            details += " evicted=\"transitive\"";
                        }
                    }
                    if (dep.hasProblem()) {
                        details += " error=\""+dep.getProblem().getMessage()+"\"";
                    }
                    if (md != null && md.getHomePage() != null) {
                        details += " homepage=\""+md.getHomePage()+"\"";
                    }
					String defaultValue = dep.getDescriptor() != null ? " default=\""+dep.getDescriptor().isDefault()+"\"" : "";
                    out.println("\t\t\t<revision name=\""+dep.getResolvedId().getRevision()+"\"" +
							 details +
                             " downloaded=\""+dep.isDownloaded()+"\""+
                             " searched=\""+dep.isSearched()+"\""+
                             defaultValue+
							 " conf=\""+toString(dep.getConfigurations(report.getConfiguration()))+"\">");
                    if (md != null) {
                        License[] licenses = md.getLicenses();
                        for (int i = 0; i < licenses.length; i++) {
                            String lurl;
                            if (licenses[i].getUrl() != null) {
                                lurl = " url=\""+licenses[i].getUrl()+"\"";
                            } else {
                                lurl = "";
                            }
                            out.println("\t\t\t\t<license name=\""+licenses[i].getName()+"\""+lurl+"/>");
                        }
                    }
                    if (dep.isEvicted(report.getConfiguration())) {
                        IvyNode.EvictionData ed = dep.getEvictedData(report.getConfiguration());
                        Collection selected = ed.getSelected();
                        if (selected != null) {
                            for (Iterator it3 = selected.iterator(); it3.hasNext();) {
                                IvyNode sel = (IvyNode)it3.next();
                                out.println("\t\t\t\t<evicted-by rev=\""+sel.getResolvedId().getRevision()+"\"/>");
                            }
                        }
                    }
                    IvyNode.Caller[] callers = dep.getCallers(report.getConfiguration());
					for (int i = 0; i < callers.length; i++) {
						out.println("\t\t\t\t<caller organisation=\""+callers[i].getModuleRevisionId().getOrganisation()+"\"" +
								" name=\""+callers[i].getModuleRevisionId().getName()+"\"" +
                                " conf=\""+toString(callers[i].getCallerConfigurations())+"\""+
                                " rev=\""+callers[i].getAskedDependencyId().getRevision()+"\"/>");
					}
					ArtifactDownloadReport[] adr = report.getDownloadReports(dep.getResolvedId());
					out.println("\t\t\t\t<artifacts>");
					for (int i = 0; i < adr.length; i++) {
						out.print("\t\t\t\t\t<artifact name=\""+adr[i].getName()+"\" type=\""+adr[i].getType()+"\" ext=\""+adr[i].getExt()+"\"");
                        out.print(" status=\""+adr[i].getDownloadStatus()+"\"");
                        out.print(" size=\""+adr[i].getSize()+"\"");
						out.println("/>");
					}
					out.println("\t\t\t\t</artifacts>");
					out.println("\t\t\t</revision>");
				}
				out.println("\t\t</module>");
			}
    		out.println("\t</dependencies>");
    		
    		out.println("</ivy-report>");
    		Message.verbose("\treport for "+mrid+" "+report.getConfiguration()+" produced in "+reportFile);
            
            File reportXsl = new File(destDir, "ivy-report.xsl");
            File reportCss = new File(destDir, "ivy-report.css");
            if (!reportXsl.exists()) {
                FileUtil.copy(XmlReportOutputter.class.getResource("ivy-report.xsl"), reportXsl, null);
            }
            if (!reportCss.exists()) {
                FileUtil.copy(XmlReportOutputter.class.getResource("ivy-report.css"), reportCss, null);
            }
    	} catch (IOException ex) {
    		Message.error("impossible to produce report for "+report.getModuleDescriptor()+": "+ex.getMessage());
    	} finally {
    		if (out != null) {
    			out.close();
    		}
    	}
    }

	private String toString(String[] strs) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < strs.length; i++) {
			buf.append(strs[i]);
			if (i + 1 < strs.length) {
				buf.append(", ");
			}
		}
		return buf.toString();
	}

	public static String getReportFileName(ConfigurationResolveReport report) {
		return getReportFileName(report.getModuleDescriptor().getModuleRevisionId().getModuleId(), 
		        report.getConfiguration());
	}

	public static String getReportFileName(ModuleId mid, String conf) {
		return 
			mid.getOrganisation()
			+ "-" + mid.getName()
			+ "-" + conf+".xml";
	}

}
