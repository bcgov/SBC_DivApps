package org.camunda.bpm.extension.hooks.services;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.identity.User;

/**
 * This class aimed at centralizing all user related information.
 *
 * @author sumathi.thirumani@aot-technologies.com
 */
public interface IUser {

    default String getName(DelegateExecution execution, String userId) {
        User user = execution.getProcessEngine().getIdentityService().createUserQuery().userId(userId).singleResult();
        return user.getFirstName()+" "+user.getLastName();
    }

    default String getEmail(DelegateExecution execution,String userId) {
        User user = execution.getProcessEngine().getIdentityService().createUserQuery().userId(userId).singleResult();
        return user.getEmail();
    }




}
