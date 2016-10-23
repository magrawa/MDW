/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.variable;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.translator.VariableTranslator;

/**
 * Represent a variable instance with its runtime value.
 */
public class VariableInstance implements Jsonable, Serializable, Comparable<VariableInstance>
{
    private String name;
    private String value;
    transient private Object data;
    private String type;
    private Long instanceId;
    private Long variableId;
    private Long processInstanceId;

    public VariableInstance() {
    }

    public VariableInstance(String name, Object objValue) {
        this.name = name;
        this.data = objValue;
    }

    public VariableInstance(JSONObject json) throws JSONException {
        if (json.has("id"))
            instanceId = json.getLong("id");
        if (json.has("variableId"))
            variableId = json.getLong("variableId");
        if (json.has("name"))
            name = json.getString("name");
        if (json.has("value"))
            value = json.getString("value");
        if (json.has("type"))
            type = json.getString("type");
    }

    public void setName(String name){
        this.name = name;
    }

    public String getName(){
        return this.name;
    }

    public void setStringValue(String value){
        this.value = value;
        this.data = null;
    }

    public String getStringValue(){
        if (value != null)
            return this.value;
        if (data == null)
            return null;
        value = VariableTranslator.toString(type, data);
        return value;
    }

    public void setData(Object data){
        this.data = data;
        this.value = null;
    }

    public Object getData(){
        if (data!=null) return this.data;
        if (value==null) return null;
        data = VariableTranslator.toObject(type, value);
        return data;
    }

    public void setType(String type){
        this.type = type;
    }

    public String getType(){
        return this.type;
    }
    public void setInstanceId(Long instanceId){
        this.instanceId = instanceId;
    }

    public Long getInstanceId(){
        return this.instanceId;
    }

    public void setVariableId(Long variableId){
        this.variableId = variableId;
    }

    public Long getVariableId(){
        return this.variableId;
    }

    public Long getProcessInstanceId() {
        return this.processInstanceId;
    }

    public void setProcessInstanceId(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String toString(){
        StringBuffer sb = new StringBuffer("");
        sb.append(" Variable Name ");
        sb.append(this.name);
        sb.append(", Variable Data ");
        sb.append(getStringValue());
        return sb.toString();
    }

    public int compareTo(VariableInstance other)
    {
      return getName().compareTo(other.getName());
    }

    public boolean isDocument() {
        if (data!=null) return data instanceof DocumentReference;
        if (value==null) return false;
        if (!value.startsWith("DOCUMENT:")) return false;
        return VariableTranslator.isDocumentReferenceVariable(null, this.type);
    }

    public Long getDocumentId() {
        if (data instanceof DocumentReference) {
            DocumentReference docRef = (DocumentReference) data;
            return docRef.getDocumentId();
        }
        if (value.startsWith("DOCUMENT:")) {
            return Long.parseLong(value.split(":")[1]);
        }
        return null;
    }

    public String getJsonName() {
        return "VariableInstance";
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (instanceId != null)
            json.put("id", instanceId);
        if (variableId != null)
            json.put("variableId", variableId);
        if (name != null)
            json.put("name", name);
        if (value != null)
            json.put("value", value);
        if (type != null)
            json.put("type", type);
        return json;
    }
}