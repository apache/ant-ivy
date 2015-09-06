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
package org.apache.ivy.ant;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Vector;

import org.apache.ivy.util.FileUtil;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.DemuxInputStream;
import org.apache.tools.ant.DemuxOutputStream;
import org.apache.tools.ant.Main;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;

import junit.framework.TestCase;

public class AntCallTriggerTest extends TestCase {
    public void test() throws Exception {
        assertFalse(new File("test/triggers/ant-call/A/out/foo.txt").exists());
        runAnt(new File("test/triggers/ant-call/A/build.xml"), "resolve");
        // should have unzipped foo.zip
        assertTrue(new File("test/triggers/ant-call/A/out/foo.txt").exists());
    }

    protected void tearDown() throws Exception {
        FileUtil.forceDelete(new File("test/triggers/ant-call/A/out"));
        FileUtil.forceDelete(new File("test/triggers/ant-call/cache"));
    }

    private void runAnt(File buildFile, String target) throws BuildException {
        runAnt(buildFile, target, Project.MSG_INFO);
    }

    private void runAnt(File buildFile, String target, int messageLevel) throws BuildException {
        Vector targets = new Vector();
        targets.add(target);
        runAnt(buildFile, targets, messageLevel);
    }

    private void runAnt(File buildFile, Vector targets, int messageLevel) throws BuildException {
        runBuild(buildFile, targets, messageLevel);

        // this exits the jvm at the end of the call
        // Main.main(new String[] {"-f", buildFile.getAbsolutePath(), target});

        // this does not set the good message level
        // Ant ant = new Ant();
        // Project project = new Project();
        // project.setBaseDir(buildFile.getParentFile());
        // project.init();
        //
        // ant.setProject(project);
        // ant.setTaskName("ant");
        //
        // ant.setAntfile(buildFile.getAbsolutePath());
        // ant.setInheritAll(false);
        // if (target != null) {
        // ant.setTarget(target);
        // }
        // ant.execute();
    }

    // ////////////////////////////////////////////////////////////////////////////
    // miserable copy (updated to simple test cases) from ant Main class:
    // the only available way I found to easily run ant exits jvm at the end
    private void runBuild(File buildFile, Vector targets, int messageLevel) throws BuildException {

        final Project project = new Project();
        project.setCoreLoader(null);

        Throwable error = null;

        try {
            addBuildListeners(project, messageLevel);
            addInputHandler(project, null);

            PrintStream err = System.err;
            PrintStream out = System.out;
            InputStream in = System.in;

            // use a system manager that prevents from System.exit()
            SecurityManager oldsm = null;
            oldsm = System.getSecurityManager();

            // SecurityManager can not be installed here for backwards
            // compatibility reasons (PD). Needs to be loaded prior to
            // ant class if we are going to implement it.
            // System.setSecurityManager(new NoExitSecurityManager());
            try {
                project.setDefaultInputStream(System.in);
                System.setIn(new DemuxInputStream(project));
                System.setOut(new PrintStream(new DemuxOutputStream(project, false)));
                System.setErr(new PrintStream(new DemuxOutputStream(project, true)));

                project.fireBuildStarted();

                project.init();
                project.setUserProperty("ant.version", Main.getAntVersion());

                project.setUserProperty("ant.file", buildFile.getAbsolutePath());

                ProjectHelper.configureProject(project, buildFile);

                // make sure that we have a target to execute
                if (targets.size() == 0) {
                    if (project.getDefaultTarget() != null) {
                        targets.addElement(project.getDefaultTarget());
                    }
                }

                project.executeTargets(targets);
            } finally {
                // put back the original security manager
                // The following will never eval to true. (PD)
                if (oldsm != null) {
                    System.setSecurityManager(oldsm);
                }

                System.setOut(out);
                System.setErr(err);
                System.setIn(in);
            }
        } catch (RuntimeException exc) {
            error = exc;
            throw exc;
        } catch (Error err) {
            error = err;
            throw err;
        } finally {
            project.fireBuildFinished(error);
        }
    }

    /**
     * Adds the listeners specified in the command line arguments, along with the default listener,
     * to the specified project.
     * 
     * @param project
     *            The project to add listeners to. Must not be <code>null</code>.
     */
    protected void addBuildListeners(Project project, int level) {

        // Add the default listener
        project.addBuildListener(createLogger(level));

    }

    /**
     * Creates the InputHandler and adds it to the project.
     * 
     * @param project
     *            the project instance.
     * @param inputHandlerClassname
     * @exception BuildException
     *                if a specified InputHandler implementation could not be loaded.
     */
    private void addInputHandler(Project project, String inputHandlerClassname)
            throws BuildException {
        InputHandler handler = null;
        if (inputHandlerClassname == null) {
            handler = new DefaultInputHandler();
        } else {
            try {
                handler = (InputHandler) (Class.forName(inputHandlerClassname).newInstance());
                if (project != null) {
                    project.setProjectReference(handler);
                }
            } catch (ClassCastException e) {
                String msg = "The specified input handler class " + inputHandlerClassname
                        + " does not implement the InputHandler interface";
                throw new BuildException(msg);
            } catch (Exception e) {
                String msg = "Unable to instantiate specified input handler " + "class "
                        + inputHandlerClassname + " : " + e.getClass().getName();
                throw new BuildException(msg);
            }
        }
        project.setInputHandler(handler);
    }

    // XXX: (Jon Skeet) Any reason for writing a message and then using a bare
    // RuntimeException rather than just using a BuildException here? Is it
    // in case the message could end up being written to no loggers (as the
    // loggers could have failed to be created due to this failure)?
    /**
     * Creates the default build logger for sending build events to the ant log.
     * 
     * @return the logger instance for this build.
     */
    private BuildLogger createLogger(int level) {
        BuildLogger logger = null;
        logger = new DefaultLogger();

        logger.setMessageOutputLevel(level);
        logger.setOutputPrintStream(System.out);
        logger.setErrorPrintStream(System.err);

        return logger;
    }

}
