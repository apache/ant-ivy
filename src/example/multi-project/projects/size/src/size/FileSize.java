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
package size;

import java.io.File;
import java.util.Collection;

import static list.ListFile.list;
import static version.Version.register;

public final class FileSize {
  static {
    register("size");
  }

  @SuppressWarnings("unused")
  public static long totalSize(File dir) {
    return totalSize(list(dir));
  }

  public static long totalSize(Collection<File> files) {
    long total = 0;
    for (File file : files) {
      total += file.length();
    }
    return total;
  }

  private FileSize() {
  }
}
