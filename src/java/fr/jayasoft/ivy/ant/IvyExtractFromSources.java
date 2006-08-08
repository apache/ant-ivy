/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.filters.LineContainsRegExp;
import org.apache.tools.ant.filters.TokenFilter;
import org.apache.tools.ant.taskdefs.Concat;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.types.RegularExpression;

import fr.jayasoft.ivy.ModuleRevisionId;

/**
 * Extracts imports from a set of java sources and generate corresponding
 * ivy file
 * 
 * @author Xavier Hanin
 *
 */
public class IvyExtractFromSources extends IvyTask {
    public static class Ignore {
        String _package;
        public String getPackage() {
            return _package;
        }
        public void setPackage(String package1) {
            _package = package1;
        }
    }
    private String  _organisation;
    private String  _module;
    private String  _revision;
    private String  _status;
    private List    _ignoredPackaged = new ArrayList(); // List (String package)
    private Map		_mapping = new HashMap(); // Map (String package -> ModuleRevisionId)
    private Concat  _concat = new Concat();
    private File    _to;
    
    public void addConfiguredIgnore(Ignore ignore) {
        _ignoredPackaged.add(ignore.getPackage());
    }
    public File getTo() {
        return _to;
    }
    public void setTo(File to) {
        _to = to;
    }
    public String getModule() {
        return _module;
    }
    public void setModule(String module) {
        _module = module;
    }
    public String getOrganisation() {
        return _organisation;
    }
    public void setOrganisation(String organisation) {
        _organisation = organisation;
    }
    public String getRevision() {
        return _revision;
    }
    public void setRevision(String revision) {
        _revision = revision;
    }
    public String getStatus() {
        return _status;
    }
    public void setStatus(String status) {
        _status = status;
    }
    public void addConfiguredMapping(PackageMapping mapping) {
        _mapping.put(mapping.getPackage(), mapping.getModuleRevisionId());
    }
    public void addFileSet(FileSet fileSet) {
        _concat.addFileset(fileSet);
    }
    
    public void execute() throws BuildException {
        configureConcat();
        Writer out = new StringWriter();
        _concat.setWriter(out);
        _concat.execute();
        Set importsSet = new HashSet(Arrays.asList(out.toString().split("\n")));
        Set dependencies = new HashSet();
        for (Iterator iter = importsSet.iterator(); iter.hasNext();) {
            String pack = ((String)iter.next()).trim();
            ModuleRevisionId mrid = getMapping(pack);
            if (mrid != null) {
                dependencies.add(mrid);
            }
        }
        try {
            PrintWriter writer = new PrintWriter(new FileOutputStream(_to));
            writer.println("<ivy-module version=\"1.0\">");
            writer.println("\t<info organisation=\""+_organisation+"\"");
            writer.println("\t       module=\""+_module+"\"");
            if (_revision != null) {
                writer.println("\t       revision=\""+_revision+"\"");
            }
            if (_status != null) {
                writer.println("\t       status=\""+_status+"\"");
            } else {
                writer.println("\t       status=\"integration\"");
            }
            writer.println("\t/>");
            if (!dependencies.isEmpty()) {
                writer.println("\t<dependencies>");
                for (Iterator iter = dependencies.iterator(); iter.hasNext();) {
                    ModuleRevisionId mrid = (ModuleRevisionId)iter.next();
                    writer.println("\t\t<dependency org=\""+mrid.getOrganisation()+"\" name=\""+mrid.getName()+"\" rev=\""+mrid.getRevision()+"\"/>");
                }
                writer.println("\t</dependencies>");
            }
            writer.println("</ivy-module>");
            writer.close();
            log(dependencies.size()+" dependencies put in "+_to);
        } catch (FileNotFoundException e) {
            throw new BuildException("impossible to create file "+_to+": "+e, e); 
        }
    }
    
    /**
     * @param pack
     * @return
     */
    private ModuleRevisionId getMapping(String pack) {
        String askedPack = pack;
        ModuleRevisionId ret = null;
        while (ret == null && pack.length() > 0) {
            if (_ignoredPackaged.contains(pack)) {
                return null;
            }
            ret = (ModuleRevisionId)_mapping.get(pack);
            int lastDotIndex = pack.lastIndexOf('.');
            if (lastDotIndex != -1) {
                pack = pack.substring(0, lastDotIndex);
            } else {
                break;
            }
        }
        if (ret == null) {
            log("no mapping found for "+askedPack, Project.MSG_VERBOSE);            
        }
        return ret;
    }
    private void configureConcat() {
        _concat.setProject(getProject());
        _concat.setTaskName(getTaskName());
        FilterChain filterChain = new FilterChain();
        LineContainsRegExp lcre = new LineContainsRegExp();
        RegularExpression regexp = new RegularExpression();
        regexp.setPattern("^import .+;");
        lcre.addConfiguredRegexp(regexp);
        filterChain.add(lcre);
        TokenFilter tf = new TokenFilter();
        TokenFilter.ReplaceRegex rre = new TokenFilter.ReplaceRegex();
        rre.setPattern("import (.+);.*");
        rre.setReplace("\\1");
        tf.add(rre);
        filterChain.add(tf);
        _concat.addFilterChain(filterChain);
    }
}
