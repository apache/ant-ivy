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
package org.apache.ivy.util;

import java.util.Iterator;
import java.util.List;

public final class MessageLoggerHelper {
    public static void sumupProblems(MessageLogger logger) {
        List myProblems = logger.getProblems();
        if (myProblems.size() > 0) {
            logger.info("\n:: problems summary ::");
            List myWarns = logger.getWarns();
            if (myWarns.size() > 0) {
                logger.info(":::: WARNINGS");
                for (Iterator iter = myWarns.iterator(); iter.hasNext();) {
                    String msg = (String) iter.next();
                    logger.log("\t" + msg + "\n", Message.MSG_WARN);
                }
            }
            List myErrors = logger.getErrors();
            if (myErrors.size() > 0) {
                logger.info(":::: ERRORS");
                for (Iterator iter = myErrors.iterator(); iter.hasNext();) {
                    String msg = (String) iter.next();
                    logger.log("\t" + msg + "\n", Message.MSG_ERR);
                }
            }
            logger.info("\n:: USE VERBOSE OR DEBUG MESSAGE LEVEL FOR MORE DETAILS");
        }
    }

    private MessageLoggerHelper() {
    }
}
