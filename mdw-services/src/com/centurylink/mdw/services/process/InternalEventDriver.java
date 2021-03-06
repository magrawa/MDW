/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.services.process;

import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class InternalEventDriver implements Runnable {

    private String eventMessage;
    private String messageId;

    public InternalEventDriver(String messageId, String eventMessage) {
        this.messageId = messageId;
        this.eventMessage = eventMessage;
    }

    public void run() {
        StandardLogger logger = LoggerUtil.getStandardLogger();
        String logtag = "EngineDriver.T" + Thread.currentThread().getId() + " - ";
        try {
            logger.info(logtag + "starts processing");
            ProcessEngineDriver driver = new ProcessEngineDriver();
            driver.processEvents(messageId, eventMessage);
        } catch (Throwable e) {    // only possible when failed to get ProcessManager ejb
            logger.severeException(logtag + "process exception " + e.getMessage(), e);
        }
    }

}