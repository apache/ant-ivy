/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.plugins.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.module.descriptor.License;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.IvyNodeCallers.Caller;
import org.apache.ivy.core.resolve.IvyNodeEviction.EvictionData;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.StringUtils;


/**
 * @author Xavier Hanin
 *
 */
public class XmlReportOutputter implements ReportOutputter {

   public String getName() {
       return XML;
   }
   
    public void output(ResolveReport report, File destDir) {
        String[] confs = report.getConfigurations();
        for (int i = 0; i < confs.length; i++) {
            output(report.getConfigurationReport(confs[i]), report.getResolveId(), confs, destDir);
        }
    }
    
    public void output(ConfigurationResolveReport report, String resolveId, String[] confs, File destDir) {
    	try {
    	    destDir.mkdirs();
    	    CacheManager cacheMgr = new CacheManager(null, destDir);
    		File reportFile = cacheMgr.getConfigurationResolveReportInCache(resolveId, report.getConfiguration());
    		OutputStream stream = new FileOutputStream(reportFile);
    		output(report, confs, stream);
    		stream.close();
    		
    		Message.verbose("\treport for "+report.getModuleDescriptor().getModuleRevisionId()+" "+report.getConfiguration()+" produced in "+reportFile);
            
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
		}
    }
    
    public void output(ConfigurationResolveReport report, OutputStream stream) {
    	output(report, new String[] {report.getConfiguration()}, stream);
    }
    public void output(ConfigurationResolveReport report, String[] confs, OutputStream stream) {
    	PrintWriter out = new PrintWriter(stream);
		ModuleRevisionId mrid = report.getModuleDescriptor().getModuleRevisionId();
        out.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
        out.println("<?xml-stylesheet type=\"text/xsl\" href=\"ivy-report.xsl\"?>");
		out.println("<ivy-report version=\"1.0\">");
		out.println("\t<info");
		out.println("\t\torganisation=\""+mrid.getOrganisation()+"\"");
		out.println("\t\tmodule=\""+mrid.getName()+"\"");
		out.println("\t\trevision=\""+mrid.getRevision()+"\"");
		if (mrid.getBranch() != null) {
			out.println("\t\tbranch=\""+mrid.getBranch()+"\"");
		}
		Map extraAttributes = mrid.getExtraAttributes();
		for (Iterator it = extraAttributes.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry entry = (Entry) it.next();
			out.println("\t\textra-" + entry.getKey() + "=\"" + entry.getValue() + "\"");
		}
		out.println("\t\tconf=\""+report.getConfiguration()+"\"");
		out.println("\t\tconfs=\""+StringUtils.join(confs, ", ")+"\"");
		out.println("\t\tdate=\""+Ivy.DATE_FORMAT.format(report.getDate())+"\"/>");
		
		out.println("\t<dependencies>");
		
		// create a list of ModuleRevisionIds indicating the position for each dependency
		List dependencies = new ArrayList(report.getModuleRevisionIds());
    	
		for (Iterator iter = report.getModuleIds().iterator(); iter.hasNext();) {
			ModuleId mid = (ModuleId) iter.next();
			out.println("\t\t<module organisation=\""+mid.getOrganisation()+"\"" +
					" name=\""+mid.getName()+"\"" +
					" resolver=\""+report.getResolveEngine().getSettings().getResolverName(mid)+"\">");
			for (Iterator it2 = report.getNodes(mid).iterator(); it2.hasNext();) {
                IvyNode dep = (IvyNode)it2.next();
                ModuleDescriptor md = null;
                if (dep.getModuleRevision() != null) {
                    md = dep.getModuleRevision().getDescriptor();
                }
                StringBuffer details = new StringBuffer();
                if  (dep.isLoaded()) {
                    details.append(" status=\"").append(dep.getDescriptor().getStatus()).append("\"").append(
                        " pubdate=\"").append(Ivy.DATE_FORMAT.format(new Date(dep.getPublication()))).append("\"").append(
                        " resolver=\"").append(dep.getModuleRevision().getResolver().getName()).append("\"").append(
                        " artresolver=\"").append(dep.getModuleRevision().getArtifactResolver().getName()).append("\"");
                }
                if (dep.isEvicted(report.getConfiguration())) {
                    EvictionData ed = dep.getEvictedData(report.getConfiguration());
                    if (ed.getConflictManager() != null) {
                        details.append(" evicted=\"").append(ed.getConflictManager()).append("\"");
                    } else {
                        details.append(" evicted=\"transitive\"");
                    }
                }
                if (dep.hasProblem()) {
                    details.append(" error=\"").append(dep.getProblem().getMessage()).append("\"");
                }
                if (md != null && md.getHomePage() != null) {
                    details.append(" homepage=\"").append(md.getHomePage()).append("\"");
                }
                extraAttributes = md!=null?md.getExtraAttributes():dep.getResolvedId().getExtraAttributes();
                for (Iterator iterator = extraAttributes.keySet().iterator(); iterator.hasNext();) {
                    String attName = (String)iterator.next();
                    details.append(" extra-").append(attName).append("=\"").append(extraAttributes.get(attName)).append("\"");
                }
				String defaultValue = dep.getDescriptor() != null ? " default=\""+dep.getDescriptor().isDefault()+"\"" : "";
                int position = dependencies.indexOf(dep.getResolvedId());
                out.println("\t\t\t<revision name=\""+dep.getResolvedId().getRevision()+"\"" +
                		 (dep.getResolvedId().getBranch() == null?"":" branch=\""+dep.getResolvedId().getBranch()+"\"")+
						 details +
                         " downloaded=\""+dep.isDownloaded()+"\""+
                         " searched=\""+dep.isSearched()+"\""+
                         defaultValue+
                         " conf=\""+toString(dep.getConfigurations(report.getConfiguration()))+"\""+
                         " position=\""+position+"\">");
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
                    EvictionData ed = dep.getEvictedData(report.getConfiguration());
                    Collection selected = ed.getSelected();
                    if (selected != null) {
                        for (Iterator it3 = selected.iterator(); it3.hasNext();) {
                            IvyNode sel = (IvyNode)it3.next();
                            out.println("\t\t\t\t<evicted-by rev=\""+sel.getResolvedId().getRevision()+"\"/>");
                        }
                    }
                }
                Caller[] callers = dep.getCallers(report.getConfiguration());
				for (int i = 0; i < callers.length; i++) {
					StringBuffer callerDetails = new StringBuffer();
					Map callerExtraAttributes = callers[i].getDependencyDescriptor().getExtraAttributes();
	                for (Iterator iterator = callerExtraAttributes.keySet().iterator(); iterator.hasNext();) {
	                    String attName = (String)iterator.next();
	                    callerDetails.append(" extra-").append(attName).append("=\"").append(callerExtraAttributes.get(attName)).append("\"");
	                }

					out.println("\t\t\t\t<caller organisation=\""+callers[i].getModuleRevisionId().getOrganisation()+"\"" +
							" name=\""+callers[i].getModuleRevisionId().getName()+"\"" +
                            " conf=\""+toString(callers[i].getCallerConfigurations())+"\""+
                            " rev=\""+callers[i].getAskedDependencyId().getRevision()+"\""+
                            callerDetails+
                            "/>");
				}
				ArtifactDownloadReport[] adr = report.getDownloadReports(dep.getResolvedId());
				out.println("\t\t\t\t<artifacts>");
				for (int i = 0; i < adr.length; i++) {
					out.print("\t\t\t\t\t<artifact name=\""+adr[i].getName()+"\" type=\""+adr[i].getType()+"\" ext=\""+adr[i].getExt()+"\"");
                    extraAttributes = adr[i].getArtifact().getExtraAttributes();
                    for (Iterator iterator = extraAttributes.keySet().iterator(); iterator.hasNext();) {
                        String attName = (String)iterator.next();
                        out.print(" extra-"+attName+"=\""+extraAttributes.get(attName)+"\"");
                    }
                    out.print(" status=\""+adr[i].getDownloadStatus()+"\"");
                    out.print(" size=\""+adr[i].getSize()+"\"");
					
					ArtifactOrigin origin = adr[i].getArtifactOrigin();
					if (origin != null) {
						out.println(">");
						out.println("\t\t\t\t\t\t<origin-location is-local=\"" + String.valueOf(origin.isLocal()) + "\"" +
									" location=\"" + origin.getLocation() + "\"/>");
						out.println("\t\t\t\t\t</artifact>");
					} else {
						out.println("/>");
					}
				}
				out.println("\t\t\t\t</artifacts>");
				out.println("\t\t\t</revision>");
			}
			out.println("\t\t</module>");
		}
		out.println("\t</dependencies>");
		out.println("</ivy-report>");
		out.flush();
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
}
