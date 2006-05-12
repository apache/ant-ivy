/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.report;

import java.io.File;

/**
 * @author x.hanin
 *
 */
public interface ReportOutputter {
    public static final String CONSOLE = "console";
    public static final String XML = "xml";

    public abstract void output(ResolveReport report, File destDir);
    public abstract String getName();
}
