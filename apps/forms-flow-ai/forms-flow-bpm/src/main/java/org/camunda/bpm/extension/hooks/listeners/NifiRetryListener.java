package org.camunda.bpm.extension.hooks.listeners;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.runtime.Incident;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Listener to retry the instances that failed to post to nifi.
 */
public class NifiRetryListener  implements ExecutionListener {
    @Override
    public void notify(DelegateExecution execution) throws Exception {
        List<String> failedNifiInstances = execution.getProcessEngineServices().getRuntimeService()
                .createIncidentQuery().incidentMessage("Unable to connect with Nifi")
                .list().stream().map(Incident::getConfiguration).collect(Collectors.toList());
        execution.getProcessEngineServices().getManagementService().setJobRetries(failedNifiInstances, 2);
    }
}
