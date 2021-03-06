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
package com.centurylink.mdw.services;

import com.centurylink.mdw.common.MdwException;

/**
 *
 */
public class ProcessException extends MdwException {

    private static final long serialVersionUID = 1L;
    /**
     * @param pMessage
     */
    public ProcessException(String pMessage) {
        super(pMessage);
    }

    /**
     * @param pCode
     * @param pMessage
     */
    public ProcessException(int pCode, String pMessage) {
        super(pCode, pMessage);
    }

    /**
     * @param pCode
     * @param pMessage
     * @param pTh
     */
    public ProcessException(int pCode, String pMessage, Throwable pTh) {
        super(pCode, pMessage, pTh);
    }


    public ProcessException(String message, Throwable t) {
        super(message, t);
    }


}
