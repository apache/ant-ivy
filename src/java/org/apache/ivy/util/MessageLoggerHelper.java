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
package org.apache.ivy.util;

import java.util.List;

public final class MessageLoggerHelper {
    public static void sumupProblems(MessageLogger logger) {
        List<String> myProblems = logger.getProblems();
        if (myProblems.size() > 0) {
            List<String> myWarns = logger.getWarns();
            List<String> myErrors = logger.getErrors();
            logger.info(""); // new line on info to isolate error summary
            if (!myErrors.isEmpty()) {
                logger.log(":: problems summary ::", Message.MSG_ERR);
            } else {
                logger.log(":: problems summary ::", Message.MSG_WARN);
            }
            if (myWarns.size() > 0) {
                logger.log(":::: WARNINGS", Message.MSG_WARN);
                for (String msg : myWarns) {
                    logger.log("\t" + msg + "\n", Message.MSG_WARN);
                }
            }
            if (myErrors.size() > 0) {
                logger.log(":::: ERRORS", Message.MSG_ERR);
                for (String msg : myErrors) {
                    logger.log("\t" + msg + "\n", Message.MSG_ERR);
                }
            }
            logger.info("\n:: USE VERBOSE OR DEBUG MESSAGE LEVEL FOR MORE DETAILS");
        }
    }

    private MessageLoggerHelper() {
    }
}
