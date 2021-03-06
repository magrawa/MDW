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
package com.centurylink.mdw.model.workflow;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.translator.impl.JavaObjectTranslator;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.variable.XPathELResolver;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.variable.VariableTranslator;
import com.sun.el.ExpressionFactoryImpl;
import com.sun.el.ValueExpressionLiteral;

public class ProcessRuntimeContext extends ELContext implements RuntimeContext {

    protected static StandardLogger logger = LoggerUtil.getStandardLogger();

    protected Package packageVO;
    public Package getPackage() { return packageVO; }

    protected Process processVO;
    public Process getProcess() { return processVO; }

    protected ProcessInstance processInstanceVO;
    public ProcessInstance getProcessInstance() { return processInstanceVO; }

    /**
     * Purposely separate from processInstanceVO.getVariables().
     */
    private Map<String,Object> variables;
    public Map<String,Object> getVariables() { return variables; }

    public String getMasterRequestId() {
        return processInstanceVO.getMasterRequestId();
    }

    private Map<String,String> attributes;
    public Map<String,String> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<String,String>();
            for (Attribute attribute : processVO.getAttributes()) {
                attributes.put(attribute.getAttributeName(), attribute.getAttributeValue());
            }
        }
        return attributes;
    }

    public String getAttribute(String name) {
        return getAttributes().get(name);
    }

    public ProcessRuntimeContext(Package packageVO, Process processVO, ProcessInstance processInstanceVO) {
        this.packageVO = packageVO;
        this.processVO = processVO;
        this.processInstanceVO = processInstanceVO;
        this.variables = new HashMap<String,Object>();
    }

    public ProcessRuntimeContext(Package packageVO, Process processVO, ProcessInstance processInstanceVO, Map<String,Object> variables) {
        this.packageVO = packageVO;
        this.processVO = processVO;
        this.processInstanceVO = processInstanceVO;
        this.variables = variables;
    }

    public String getProperty(String name) {
        return packageVO.getProperty(name);
    }

    public Long getProcessId() {
        return processVO.getId();
    }

    public Long getProcessInstanceId() {
        return processInstanceVO.getId();
    }

    public void logInfo(String message) {
        logger.info(logtag(), message);
    }

    public void logDebug(String message) {
        logger.debug(logtag(), message);
    }

    public void logWarn(String message) {
        logger.warn(logtag(), message);
    }

    public void logSevere(String message) {
        logger.severe(logtag(), message);
    }

    public void logException(String msg, Exception e) {
        logger.exception(logtag(), msg, e);
    }

    public boolean isLogInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public boolean isLogDebugEnabled() {
        return logger.isDebugEnabled();
    }

    protected String logtag() {
        StringBuffer sb = new StringBuffer();
        sb.append("p");
        sb.append(this.getProcessId());
        sb.append(".");
        sb.append(this.getProcessInstanceId());
        return sb.toString();
    }

    public String getCompletionCode() {
        return processInstanceVO.getCompletionCode();
    }

    public void setProcessCompletionCode(String code) {
        processInstanceVO.setCompletionCode(code);
    }

    public Object evaluate(String expression) {
        ExpressionFactory factory = getExpressionFactory();
        ValueExpression valueExp = factory.createValueExpression(this, expression, Object.class);
        return valueExp.getValue(this);
    }

    public void set(String expression, Object value) {
        ExpressionFactory factory = getExpressionFactory();
        ValueExpression valueExp = factory.createValueExpression(this, expression, Object.class);
        valueExp.setValue(this, value);
    }

    public String evaluateToString(String expression) {
        Object obj = evaluate(expression);
        return obj == null ? "" : obj.toString();
    }

    private static ExpressionFactory expressionFactory;
    private CompositeELResolver elResolver;
    private VariableMapper variableMapper;
    private FunctionMapper functionMapper;

    public static ExpressionFactory getExpressionFactory() {
        if (expressionFactory == null) {
            expressionFactory = new ExpressionFactoryImpl();
        }
        return expressionFactory;
    }

    public ELResolver getELResolver() {
        if (elResolver == null) {
            elResolver = new CompositeELResolver() {
                @Override
                public Object getValue(ELContext elContext, Object base, Object property) {
                    if (base == null) {
                        elContext.setPropertyResolved(true);
                        return "";  // don't blow up on empty variables
                    }
                    else {
                        return super.getValue(elContext, base, property);
                    }
                }
            };
            elResolver.add(new XPathELResolver());
            elResolver.add(new MapELResolver());
            elResolver.add(new ListELResolver());
            elResolver.add(new ArrayELResolver());
            elResolver.add(new BeanELResolver());
        }
        return elResolver;
    }

    public VariableMapper getVariableMapper() {
        if (variableMapper == null) {
            variableMapper = new ProcessVariableMapper();
        }
        return variableMapper;
    }

    public FunctionMapper getFunctionMapper() {
        // method expressions not yet supported
        if (functionMapper == null) {
            functionMapper = new FunctionMapper() {
                public Method resolveFunction(String prefix, String localName) {
                    return null;
                }
            };
        }
        return functionMapper;
    }

    class ProcessVariableMapper extends VariableMapper {

        public ValueExpression resolveVariable(String variable) {
            return getValueExpressionMap().get(variable);
        }

        public ValueExpression setVariable(String variable, ValueExpression expression) {
            return getValueExpressionMap().put(variable, expression);
        }
    }

    private Map<String,ValueExpression> valueExpressionMap;

    protected Map<String,ValueExpression> getValueExpressionMap() {
        if (valueExpressionMap == null) {
            valueExpressionMap = new HashMap<String,ValueExpression>();
            valueExpressionMap.put("context", new ValueExpressionLiteral(this, Object.class));
            valueExpressionMap.put("masterRequestId", new ValueExpressionLiteral(getMasterRequestId(), String.class));
            valueExpressionMap.put("mdwHubUrl", new ValueExpressionLiteral(ApplicationContext.getMdwHubUrl(), String.class));
            valueExpressionMap.put("processInstanceId", new ValueExpressionLiteral(this.getProcessInstanceId(), String.class));
            valueExpressionMap.put("processName", new ValueExpressionLiteral(this.processVO.getProcessName(), String.class));
            valueExpressionMap.put("process", new ValueExpressionLiteral(this.processInstanceVO, Object.class));
            valueExpressionMap.put("variables", new ValueExpressionLiteral(this.getVariables() , Object.class));
            valueExpressionMap.put("props", new ValueExpressionLiteral(this.getPropertyAccessorMap(), Map.class));

            Map<String,Object> variables = getVariables();
            if (variables != null) {
                for (String varName : variables.keySet()) {
                    valueExpressionMap.put(varName, new ValueExpressionLiteral(variables.get(varName), Object.class));
                }
            }
        }
        return valueExpressionMap;
    }

    private Map<String,String> getPropertyAccessorMap() {
        return new HashMap<String,String>() {
            @Override
            public String get(Object key) {
                return PropertyManager.getProperty(key.toString());
            }
        };
    }

    public String getMdwHubUrl() {
        return ApplicationContext.getMdwHubUrl();
    }

    public String getServicesUrl() {
        return ApplicationContext.getServicesUrl();
    }

    public String getMdwVersion() {
        return ApplicationContext.getMdwVersion();
    }

    public boolean isExpression(String str) {
        if (str == null)
            return false;
        int start;
        return ((start = str.indexOf("#{")) != -1) && (start < str.indexOf('}')) ||
                ((start = str.indexOf("${")) != -1) && (start < str.indexOf('}'));
    }

    /**
     * Returns a variable value.  Key can be a var name or an expression.
     */
    public Object getValue(String key) {
        if (isExpression(key))
            return evaluate(key);
        else
            return getVariables().get(key);
    }

    public String getValueAsString(String key) {
        if (isExpression(key)) {
            return evaluateToString(key);
        }
        else {
            Object obj = getVariables().get(key);
            Variable var = getProcess().getVariable(key);
            if (var == null)
                throw new IllegalArgumentException("Variable not defined: " + key);
            VariableTranslator translator = com.centurylink.mdw.translator.VariableTranslator
                    .getTranslator(getPackage(), var.getVariableType());
            if (translator instanceof JavaObjectTranslator)
                return obj.toString();
            if (translator instanceof DocumentReferenceTranslator)
                return ((DocumentReferenceTranslator)translator).realToString(obj);
            else
                return translator.toString(obj);
        }
    }

    public Object getValueForString(String varName, String strVal) {
        Variable var = getProcess().getVariable(varName);
        if (var == null)
            throw new IllegalArgumentException("Variable not defined: " + varName);
        VariableTranslator translator = com.centurylink.mdw.translator.VariableTranslator
                .getTranslator(getPackage(), var.getVariableType());
        if (translator instanceof DocumentReferenceTranslator)
            return ((DocumentReferenceTranslator)translator).realToObject(strVal);
        else
            return translator.toObject(strVal);
    }
}
