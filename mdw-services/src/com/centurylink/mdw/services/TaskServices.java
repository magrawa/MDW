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

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.Value;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.task.UserTaskAction;
import com.centurylink.mdw.model.task.TaskCount;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.model.task.TaskRuntimeContext;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.task.SubTask;
import com.centurylink.mdw.task.types.TaskList;

public interface TaskServices {

    /**
     * Returns tasks (optionally associated with the specified user's workgroups).
     */
    public TaskList getTasks(Query query, String cuid) throws ServiceException;

    public TaskList getProcessTasks(Long processInstanceId) throws DataAccessException;

    public Map<String,String> getIndexes(Long taskInstanceId) throws DataAccessException;

    public TaskInstance createTaskInstance(AssetVersionSpec spec, String masterRequestId, Long processInstanceId,
            Long activityInstanceId, Long transitionId, String comments) throws TaskException, DataAccessException, CachingException;

    public TaskInstance getInstance(Long instanceId) throws DataAccessException;

    /**
     * Returns a map of name to runtime Value.
     */
    public Map<String,Value> getValues(Long instanceId) throws ServiceException;

    public void applyValues(Long instanceId, Map<String,String> values) throws ServiceException;

    /**
     * Create an ad-hoc manual task instance.
     * @return the newly-created instance id
     */
    public Long createTask(String userCuid, String logicalId) throws ServiceException;

    public void createSubTask(String subtaskLogicalId, Long masterTaskInstanceId)
    throws TaskException, DataAccessException;

    public void createSubTasks(List<SubTask> subTaskList, TaskRuntimeContext masterTaskContext)
    throws TaskException, DataAccessException;

    public TaskList getSubtasks(Long masterTaskInstanceId) throws ServiceException;

    public List<TaskTemplate> getTaskTemplates(Query query) throws ServiceException;

    public List<TaskCount> getTopThroughputTasks(String aggregateBy, Query query) throws ServiceException;

    public Map<Date,List<TaskCount>> getTaskInstanceBreakdown(Query query) throws ServiceException;

    public TaskRuntimeContext getRuntimeContext(Long instanceId) throws ServiceException;

    public void performTaskAction(UserTaskAction taskAction) throws ServiceException;

    /**
     * Update a task instance.
     */
    public void updateTask(String userCuid, TaskInstance taskInstance) throws ServiceException;
}
