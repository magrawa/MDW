/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import java.util.List;
import java.util.Map;

import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.event.ExternalEvent;
import com.centurylink.mdw.model.task.TaskCategory;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.variable.VariableType;
import com.centurylink.mdw.model.workflow.ActivityImplementor;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.util.timer.ProgressMonitor;

/**
 * common interface for all process loading
 */
public interface ProcessLoader {

    Package loadPackage(Long packageId, boolean deep)
    throws DataAccessException;

    Package getPackage(String name)
    throws DataAccessException;

    List<Package> getPackageList(boolean deep, ProgressMonitor progressMonitor)
    throws DataAccessException;

    List<Process> getProcessList()
    throws DataAccessException;

    Process loadProcess(Long processID, boolean withSubProcesses)
    throws DataAccessException;

    Process getProcessBase(Long processId)
    throws DataAccessException;

    Process getProcessBase(String name, int version)
    throws DataAccessException;

    List<ExternalEvent> loadExternalEvents()
    throws DataAccessException;

    List<TaskTemplate> getTaskTemplates()
    throws DataAccessException;

    List<ActivityImplementor> getActivityImplementors()
    throws DataAccessException;

    List<VariableType> getVariableTypes()
    throws DataAccessException;

    List<TaskCategory> getTaskCategories()
    throws DataAccessException;

    List<ActivityImplementor> getReferencedImplementors(Package packageVO)
    throws DataAccessException;

    List<Process> findCallingProcesses(Process subproc)
    throws DataAccessException;

    List<Process> findCalledProcesses(Process main)
    throws DataAccessException;

    List<Asset> getAssets()
    throws DataAccessException;

    Asset getAsset(Long assetId)
    throws DataAccessException;

    Asset getAsset(String name, String language, int version)
    throws DataAccessException;

    Asset getAsset(Long packageId, String name)
    throws DataAccessException;

    Asset getAssetForOwner(String ownerType, Long ownerId)
    throws DataAccessException;

    public List<Process> getProcessListForImplementor(Long implementorId, String implementorClass)
    throws DataAccessException;

    public Map<String,String> getAttributes(String ownerType, Long ownerId)
    throws DataAccessException;
}
