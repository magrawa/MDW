/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.process;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBElement;

import org.apache.xmlbeans.XmlObject;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.yaml.snakeyaml.Yaml;

import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.StringDocument;
import com.centurylink.mdw.model.event.EventInstance;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.services.ProcessException;
import com.centurylink.mdw.services.ProcessManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.translator.VariableTranslator;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.xml.XmlBeanWrapper;
import com.qwest.mbeng.MbengDocument;
import com.qwest.mbeng.MbengTableArray;

public class ProcessManagerBean implements ProcessManager {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public Long launchProcess(String processName, Object masterRequest, String masterRequestId,
            Map<String,Object> parameters) throws ProcessException {
        return launchProcess(processName, masterRequest, masterRequestId, parameters, null);
    }

    public Long launchProcess(String processName, Object masterRequest, String masterRequestId,
            Map<String,Object> parameters, Map<String,String> headers) throws ProcessException {

        return launchProcess(processName, masterRequest, null, masterRequestId, parameters, headers);
    }

    public Long launchProcess(String processName, Object masterRequest, String masterRequestDocType, String masterRequestId,
            Map<String,Object> parameters, Map<String,String> headers) throws ProcessException {

        try {
            Long docId = 0L;
            Process processVO = ProcessCache.getProcess(processName, 0);
            if (masterRequest != null) {
                String docType = masterRequestDocType;
                if (docType == null)
                    docType = getDocType(masterRequest);
                EventManager eventMgr = ServiceLocator.getEventManager();
                docId = eventMgr.createDocument(docType, OwnerType.LISTENER_REQUEST, 0L,
                        masterRequest, PackageCache.getProcessPackage(processVO.getId()));
                if (headers == null)
                    headers = new HashMap<String,String>();
                headers.put(Listener.METAINFO_DOCUMENT_ID, docId.toString());
            }

            Map<String,String> stringParams = translateParameters(processVO, parameters);
            ProcessEngineDriver driver = new ProcessEngineDriver();
            return driver.startProcess(processVO.getId(), masterRequestId, OwnerType.DOCUMENT, docId, stringParams, headers);
        }
        catch (Exception ex) {
            throw new ProcessException(ex.getMessage(), ex);
        }
    }

    public Object invokeServiceProcess(String processName, Object masterRequest, String masterRequestId,
            Map<String,Object> parameters) throws ProcessException {
        return invokeServiceProcess(processName, masterRequest, masterRequestId, parameters, null);
    }

    public Object invokeServiceProcess(String processName, Object masterRequest, String masterRequestId,
            Map<String,Object> parameters, Map<String,String> headers) throws ProcessException {

        return invokeServiceProcess(processName, masterRequest, null, masterRequestId, parameters, "response", headers);
    }

    public Object invokeServiceProcess(String processName, Object masterRequest, String masterRequestDocType,
            String masterRequestId, Map<String,Object> parameters, String responseVarName, Map<String,String> headers) throws ProcessException {
        try {
            Long docId = 0L;
            String request = null;
            Process processVO = ProcessCache.getProcess(processName, 0);
            Package pkg = PackageCache.getProcessPackage(processVO.getId());
            if (masterRequest != null) {
                String docType = masterRequestDocType;
                if (docType == null)
                    docType = getDocType(masterRequest);
                EventManager eventMgr = ServiceLocator.getEventManager();
                docId = eventMgr.createDocument(docType, OwnerType.LISTENER_REQUEST, 0L, masterRequest, pkg);
                request = VariableTranslator.realToString(pkg, docType, masterRequest);
                if (headers == null)
                    headers = new HashMap<String,String>();
                headers.put(Listener.METAINFO_DOCUMENT_ID, docId.toString());
            }

            Map<String,String> stringParams = translateParameters(processVO, parameters);

            ProcessEngineDriver engineDriver = new ProcessEngineDriver();
            String resp = engineDriver.invokeService(processVO.getId(), OwnerType.DOCUMENT, docId, masterRequestId,
                    request, stringParams, responseVarName, headers);
            Object response = resp;
            if (resp != null) {
                String respVar = responseVarName;
                if (respVar == null)
                    respVar = "response";
                Variable var = processVO.getVariable(respVar);
                if (var != null && var.isOutput() && !var.isString()) {
                    response = VariableTranslator.realToObject(pkg, var.getVariableType(), resp);
                }
            }
            return response;
        }
        catch (Exception ex) {
            throw new ProcessException(ex.getMessage(), ex);
        }
    }

    public Map<String,String> translateParameters(Process processVO, Map<String,Object> parameters) throws ProcessException {
        Map<String,String> stringParams = new HashMap<String,String>();
        for (String key : parameters.keySet()) {
            Object val = parameters.get(key);
            Variable vo = processVO.getVariable(key);
            if (vo == null)
              throw new ProcessException("Variable '" + key + "' not found for process: " + processVO.getProcessName() + " v" + processVO.getVersionString() + "(id=" + processVO.getId() + ")");
            String translated;
            if (val instanceof String)
                translated = (String)val;
            else
                translated = VariableTranslator.toString(PackageCache.getProcessPackage(processVO.getId()), vo.getVariableType(), val);
            stringParams.put(key, translated);
        }
        return stringParams;
    }

    public Integer notifyProcesses(String eventName, Object eventMessage) {
        Integer delay = PropertyManager.getIntegerProperty(PropertyNames.ACTIVITY_RESUME_DELAY, 2);
        return notifyProcesses(eventName, eventMessage, delay);
    }

    public Integer notifyProcesses(String eventName, Object eventMessage, int delay) {
        try {
            Long docId = 0L;
            String message = null;
            if (eventMessage != null) {
                String docType = getDocType(eventMessage);
                EventManager eventMgr = ServiceLocator.getEventManager();
                docId = eventMgr.createDocument(docType, OwnerType.LISTENER_REQUEST, 0L, eventMessage);
                message = VariableTranslator.realToString(docType, eventMessage);
            }
            EventManager eventManager = ServiceLocator.getEventManager();
            return eventManager.notifyProcess(eventName, docId, message, delay);
        } catch (Exception e) {
            logger.severeException(e.getMessage(), e);
            return EventInstance.RESUME_STATUS_FAILURE;
        }
    }


    /**
     * TODO: factor to common class in mdw-services
     */
    private String getDocType(Object docObj) {
        if (docObj instanceof String || docObj instanceof StringDocument)
            return StringDocument.class.getName();
        else if (docObj instanceof XmlObject)
            return XmlObject.class.getName();
        else if (docObj instanceof XmlBeanWrapper)
            return XmlBeanWrapper.class.getName();
        else if (docObj instanceof groovy.util.Node)
            return groovy.util.Node.class.getName();
        else if (docObj instanceof JAXBElement)
            return JAXBElement.class.getName();
        else if (docObj instanceof Document)
            return Document.class.getName();
        else if (docObj instanceof JSONObject)
            return JSONObject.class.getName();
        else if (docObj.getClass().getName().equals("org.apache.camel.component.cxf.CxfPayload"))
            return "org.apache.camel.component.cxf.CxfPayload";
        else if (docObj instanceof MbengTableArray)
            return MbengTableArray.class.getName();
        else if (docObj instanceof MbengDocument)
            return MbengDocument.class.getName();
        else if (docObj instanceof FormDataDocument)
            return FormDataDocument.class.getName();
        else if (docObj instanceof Jsonable)
            return Jsonable.class.getName();
        else if (docObj instanceof Yaml)
            return Yaml.class.getName();
        else
            return Object.class.getName();

    }

}
