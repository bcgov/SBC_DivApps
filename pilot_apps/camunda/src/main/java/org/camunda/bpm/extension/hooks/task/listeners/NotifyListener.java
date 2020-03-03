package org.camunda.bpm.extension.hooks.task.listeners;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.extension.hooks.services.IMessageEvent;


import java.util.logging.Logger;

/**
 * Assignment Task Listener to start a message event when a user is assigned
 *
 * @author yichun.zhao@aot-technologies.com
 */
public class NotifyListener implements TaskListener, IMessageEvent {

    private static final Logger log = Logger.getLogger(NotifyListener.class.getName());

    /**
     * This provides the necessary information to send message.
     *
     * @param delegateTask: The task which sends the message
     */
    public void notify(DelegateTask delegateTask) {
        log.info("\n\nNotify listener invoked! " + delegateTask.getId() + "\n\nAssigned Date set\n\n");

        String assignee = delegateTask.getAssignee();

        if (assignee != null) {
             log.info("\n\nAssigned date is " + delegateTask.getVariable("assigned_date") + "\n\n");
            sendMessage(delegateTask,"assignment_notification");
        }
    }
}
