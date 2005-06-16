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
    public abstract void output(ResolveReport report, File destDir);
}
