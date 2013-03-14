/*
 * Copyright 2013 JBoss by Red Hat.
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
package org.jbpm.task;

import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.jbpm.shared.services.api.JbpmServicesPersistenceManager;
import org.jbpm.shared.services.api.JbpmServicesTransactionManager;
import org.jbpm.shared.services.impl.JbpmLocalTransactionManager;
import org.jbpm.shared.services.impl.JbpmServicesPersistenceManagerImpl;
import org.jbpm.task.api.TaskAdminService;
import org.jbpm.task.api.TaskContentService;
import org.jbpm.task.api.TaskDeadlinesService;
import org.jbpm.task.api.TaskIdentityService;
import org.jbpm.task.api.TaskInstanceService;
import org.jbpm.task.api.TaskQueryService;
import org.jbpm.task.api.TaskServiceEntryPoint;
import org.jbpm.task.deadlines.DeadlinesDecorator;
import org.jbpm.task.identity.MvelUserGroupCallbackImpl;
import org.jbpm.task.identity.UserGroupCallback;
import org.jbpm.task.identity.UserGroupLifeCycleManagerDecorator;
import org.jbpm.task.identity.UserGroupTaskInstanceServiceDecorator;
import org.jbpm.task.identity.UserGroupTaskQueryServiceDecorator;
import org.jbpm.task.impl.TaskAdminServiceImpl;
import org.jbpm.task.impl.TaskContentServiceImpl;
import org.jbpm.task.impl.TaskDeadlinesServiceImpl;
import org.jbpm.task.impl.TaskIdentityServiceImpl;
import org.jbpm.task.impl.TaskInstanceServiceImpl;
import org.jbpm.task.impl.TaskQueryServiceImpl;
import org.jbpm.task.impl.TaskServiceEntryPointImpl;
import org.jbpm.task.internals.lifecycle.LifeCycleManager;
import org.jbpm.task.internals.lifecycle.MVELLifeCycleManager;
import org.jbpm.task.subtask.SubTaskDecorator;

/**
 *
 * @author salaboy
 */
public class HumanTaskServiceFactory {
    
    private static TaskServiceEntryPoint service;
    
    private static EntityManagerFactory emf;
    
    private static JbpmServicesTransactionManager jbpmTransactionManager = new JbpmLocalTransactionManager();
    
    private static JbpmServicesPersistenceManager pm = new JbpmServicesPersistenceManagerImpl();
    
    private static Logger logger = LogManager.getLogManager().getLogger("");
    
    private static TaskQueryService queryService = new TaskQueryServiceImpl();
    
    private static TaskIdentityService identityService = new TaskIdentityServiceImpl();
    
    private static TaskAdminService adminService = new TaskAdminServiceImpl();
    
    private static TaskContentService contentService = new TaskContentServiceImpl();
    
    private static TaskDeadlinesService deadlinesService = new TaskDeadlinesServiceImpl();
    
    private static TaskInstanceService instanceService =  new TaskInstanceServiceImpl();
    
    private static LifeCycleManager lifeCycleManager = new MVELLifeCycleManager();
    
    private static UserGroupLifeCycleManagerDecorator userGroupLifeCycleDecorator = new UserGroupLifeCycleManagerDecorator();
    
     
    public static TaskServiceEntryPoint newTaskService(){
        configure();
        return service;
    }
    
    public static JbpmServicesPersistenceManager getJbpmServicesPersistenceManager(){
        return pm;
    }
    
    
    public static void configure(){
        service = new TaskServiceEntryPointImpl();
        // Persistence and Transactions
        configurePersistenceManager();

        UserGroupCallback userGroupCallback = createUserGroupCallback();
        // Task Query
        configureTaskQueryService(pm);
        TaskQueryService userGroupQueryServiceDecorator = configureUserGroupQueryServiceDecorator(queryService, userGroupCallback);
        service.setTaskQueryService(userGroupQueryServiceDecorator);
        
        // Task Identity
        configureTaskIdentityService(pm);
        service.setTaskIdentityService(identityService);
        
        // Task Admin
        configureTaskAdminService(pm);
        service.setTaskAdminService(adminService);
        
        // Task Content
        configureTaskContentService(pm);
        service.setTaskContentService(contentService);
        
        // Task Deadlines
        configureTaskDeadlinesService(pm);

        // Task Instance
        configureTaskInstanceService(pm, queryService);
        
        // Task Instance  - Lifecycle Manager
        configureLifeCycleManager(pm, identityService, queryService, contentService);
        
        // User/Group Callbacks
        
        configureUserGroupLifeCycleManagerDecorator(pm, lifeCycleManager, userGroupCallback);
    
        ((TaskInstanceServiceImpl)instanceService).setLifeCycleManager(userGroupLifeCycleDecorator);
        
        TaskInstanceService userGroupTaskInstanceServiceDecorator = configureUserGroupTaskInstanceServiceDecorator(instanceService, userGroupCallback);
                
        // Task Decorators - Sub Tasks
        SubTaskDecorator subTaskDecorator = createSubTaskDecorator(pm, userGroupTaskInstanceServiceDecorator, queryService);
        
        // Task Decorators - Deadlines
        DeadlinesDecorator deadlinesDecorator = createDeadlinesDecorator(pm, queryService, deadlinesService, subTaskDecorator);
        
        service.setTaskInstanceService(deadlinesDecorator);
        
    }

    
    
   
    public static void setJbpmServicesTransactionManager(JbpmServicesTransactionManager txmgr){
        HumanTaskServiceFactory.jbpmTransactionManager = txmgr;
    }
    
    public static void configurePersistenceManager(){
        EntityManager em = emf.createEntityManager();
        // Persistence and Transactions
        ((JbpmServicesPersistenceManagerImpl)pm).setEm(em);
        ((JbpmServicesPersistenceManagerImpl)pm).setTransactionManager(jbpmTransactionManager);
        
    }
    
    public static void setEntityManagerFactory(EntityManagerFactory emf){
        HumanTaskServiceFactory.emf = emf;
    }
    
    public static EntityManagerFactory getEntityManagerFactory(){
        return HumanTaskServiceFactory.emf;
    }
    
    public static void configureTaskQueryService(JbpmServicesPersistenceManager pm){
        
        ((TaskQueryServiceImpl)queryService).setPm(pm);
    }
    
    public static void configureTaskIdentityService(JbpmServicesPersistenceManager pm){
        ((TaskIdentityServiceImpl)identityService).setPm(pm);
    }
    
    public static void configureTaskAdminService(JbpmServicesPersistenceManager pm){
        ((TaskAdminServiceImpl)adminService).setPm(pm);
        
    }
    
    public static void configureTaskContentService(JbpmServicesPersistenceManager pm){
        ((TaskContentServiceImpl)contentService).setPm(pm);
    }
    
    public static void configureTaskDeadlinesService(JbpmServicesPersistenceManager pm){
        ((TaskDeadlinesServiceImpl)deadlinesService).setPm(pm);
        ((TaskDeadlinesServiceImpl)deadlinesService).setLogger(logger);
        ((TaskDeadlinesServiceImpl)deadlinesService).setNotificationEvents(service.getTaskNotificationEventListeners());
        ((TaskDeadlinesServiceImpl)deadlinesService).init();
    }
    
    public static void configureTaskInstanceService(JbpmServicesPersistenceManager pm, TaskQueryService queryService){
        ((TaskInstanceServiceImpl)instanceService).setPm(pm);
        ((TaskInstanceServiceImpl)instanceService).setTaskQueryService(queryService);
        ((TaskInstanceServiceImpl)instanceService).setTaskEvents(service.getTaskLifecycleEventListeners());
    }
    
    public static void configureLifeCycleManager(JbpmServicesPersistenceManager pm, 
                    TaskIdentityService identityService, TaskQueryService queryService, TaskContentService contentService){
        
        ((MVELLifeCycleManager)lifeCycleManager).setPm(pm);
        ((MVELLifeCycleManager)lifeCycleManager).setTaskIdentityService(identityService);
        ((MVELLifeCycleManager)lifeCycleManager).setTaskQueryService(queryService);
        ((MVELLifeCycleManager)lifeCycleManager).setTaskContentService(contentService);
        ((MVELLifeCycleManager)lifeCycleManager).setTaskEvents(service.getTaskLifecycleEventListeners());
        ((MVELLifeCycleManager)lifeCycleManager).setLogger(logger);
        ((MVELLifeCycleManager)lifeCycleManager).initMVELOperations();
        
        
    }
    
    public static void configureUserGroupLifeCycleManagerDecorator(JbpmServicesPersistenceManager pm, 
                                                            LifeCycleManager lifeCycleManager, UserGroupCallback userGroupCallback){
        
        
        userGroupLifeCycleDecorator.setManager(lifeCycleManager);
        userGroupLifeCycleDecorator.setPm(pm);
        userGroupLifeCycleDecorator.setUserGroupCallback(userGroupCallback);
        
    }
    
    public static UserGroupCallback createUserGroupCallback(){
        return new MvelUserGroupCallbackImpl();
    }
    
    public static SubTaskDecorator createSubTaskDecorator(JbpmServicesPersistenceManager pm, 
                                            TaskInstanceService instanceService, TaskQueryService queryService){
        SubTaskDecorator subTaskDecorator = new SubTaskDecorator();
        subTaskDecorator.setPm(pm);
        subTaskDecorator.setInstanceService(instanceService);
        subTaskDecorator.setQueryService(queryService);
        return subTaskDecorator;
    }
    
    public static DeadlinesDecorator createDeadlinesDecorator(JbpmServicesPersistenceManager pm, TaskQueryService queryService,
                                                TaskDeadlinesService deadlinesService, SubTaskDecorator subTaskDecorator){
        DeadlinesDecorator deadlinesDecorator = new DeadlinesDecorator();
        deadlinesDecorator.setPm(pm);
        deadlinesDecorator.setQueryService(queryService);
        deadlinesDecorator.setDeadlineService(deadlinesService);
        deadlinesDecorator.setInstanceService(subTaskDecorator);
        return deadlinesDecorator;
    
    }
    
    private static TaskQueryService configureUserGroupQueryServiceDecorator(TaskQueryService queryService, UserGroupCallback userGroupCallback) {
        UserGroupTaskQueryServiceDecorator userGroupTaskQueryServiceDecorator = new UserGroupTaskQueryServiceDecorator();
        userGroupTaskQueryServiceDecorator.setPm(pm);
        userGroupTaskQueryServiceDecorator.setUserGroupCallback(userGroupCallback);
        userGroupTaskQueryServiceDecorator.setDelegate(queryService);
        return userGroupTaskQueryServiceDecorator;
        
    }
    
    private static TaskInstanceService configureUserGroupTaskInstanceServiceDecorator(TaskInstanceService instanceService, UserGroupCallback userGroupCallback) {
        UserGroupTaskInstanceServiceDecorator userGroupTaskInstanceDecorator = new UserGroupTaskInstanceServiceDecorator();
        userGroupTaskInstanceDecorator.setPm(pm);
        userGroupTaskInstanceDecorator.setUserGroupCallback(userGroupCallback);
        userGroupTaskInstanceDecorator.setDelegate(instanceService);
        return userGroupTaskInstanceDecorator;
    }

    public static void setQueryService(TaskQueryService queryService) {
        HumanTaskServiceFactory.queryService = queryService;
    }

    public static void setIdentityService(TaskIdentityService identityService) {
        HumanTaskServiceFactory.identityService = identityService;
    }

    public static void setAdminService(TaskAdminService adminService) {
        HumanTaskServiceFactory.adminService = adminService;
    }

    public static void setContentService(TaskContentService contentService) {
        HumanTaskServiceFactory.contentService = contentService;
    }

    public static void setDeadlinesService(TaskDeadlinesService deadlinesService) {
        HumanTaskServiceFactory.deadlinesService = deadlinesService;
    }

    public static void setInstanceService(TaskInstanceService instanceService) {
        HumanTaskServiceFactory.instanceService = instanceService;
    }

    public static void setLifeCycleManager(LifeCycleManager lifeCycleManager) {
        HumanTaskServiceFactory.lifeCycleManager = lifeCycleManager;
    }

    public static void setUserGroupLifeCycleDecorator(UserGroupLifeCycleManagerDecorator userGroupLifeCycleDecorator) {
        HumanTaskServiceFactory.userGroupLifeCycleDecorator = userGroupLifeCycleDecorator;
    }

    public static EntityManagerFactory getEmf() {
        return HumanTaskServiceFactory.emf;
    }

    public static JbpmServicesTransactionManager getJbpmTransactionManager() {
        return HumanTaskServiceFactory.jbpmTransactionManager;
    }

    public static TaskQueryService getQueryService() {
        return HumanTaskServiceFactory.queryService;
    }

    public static TaskIdentityService getIdentityService() {
        return HumanTaskServiceFactory.identityService;
    }

    public static TaskAdminService getAdminService() {
        return HumanTaskServiceFactory.adminService;
    }

    public static TaskContentService getContentService() {
        return HumanTaskServiceFactory.contentService;
    }

    public static TaskDeadlinesService getDeadlinesService() {
        return HumanTaskServiceFactory.deadlinesService;
    }

    public static TaskInstanceService getInstanceService() {
        return HumanTaskServiceFactory.instanceService;
    }

    public static LifeCycleManager getLifeCycleManager() {
        return HumanTaskServiceFactory.lifeCycleManager;
    }

    public static UserGroupLifeCycleManagerDecorator getUserGroupLifeCycleDecorator() {
        return HumanTaskServiceFactory.userGroupLifeCycleDecorator;
    }

    
    
    
}
