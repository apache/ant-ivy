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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Delete;

/**
 * Cleans the content of Ivy cache.
 * 
 *  The whole directory used as Ivy cache is deleted, this is roughly equivalent to:
 *  <pre>
 *  &lt;delete dir="${ivy.cache.dir}" &gt;
 *  </pre>
 *  
 *  Using the delete task gives more control over what is actually deleted (you can use include
 *  and exclude filters), but requires a settings to be loaded before, while this task
 *  ensures the settings is loaded.
 */
public class IvyCleanCache extends IvyTask {

    public void doExecute() throws BuildException {
        Delete delete = new Delete();
        delete.setTaskName(getTaskName());
        delete.setProject(getProject());
        delete.setDir(getIvyInstance().getSettings().getDefaultCache());
        delete.perform();
    }
}
