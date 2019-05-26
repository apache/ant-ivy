/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package find;

import java.util.Collection;
import java.io.File;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;

import static list.ListFile.list;
import static version.Version.register;

public final class FindFile {
  static {
    register("find");
  }

  public static Collection<File> find(File dir, String name) {
    return find(list(dir), name);
  }

  private static Collection<File> find(Collection<File> files, final String name) {
    return CollectionUtils.select(files, new Predicate<File>() {
      public boolean evaluate(File file) {
        return file.getName().contains(name);
      }
    });
  }

  private FindFile() {
  }
}
