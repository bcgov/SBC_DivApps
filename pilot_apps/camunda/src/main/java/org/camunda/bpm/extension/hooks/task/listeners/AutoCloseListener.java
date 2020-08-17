package org.camunda.bpm.extension.hooks.task.listeners;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.extension.hooks.services.analytics.SimpleDBDataPipeline;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Named;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This class has been written specific to process, and to be enhanced to give generic behavior.
 *
 * @author sumathi.thirumani@aot-technologies.com
 */
@Named("autocloseDelegate")
public class AutoCloseListener implements ExecutionListener {

    @Autowired
    private SimpleDBDataPipeline dbdatapipeline;

    private final Logger LOGGER = Logger.getLogger(AutoCloseListener.class.getName());

    @Override
    public void notify(DelegateExecution execution) throws Exception {
        Integer expireInDays = execution.getVariables().containsKey("expireInDays") ? (Integer) execution.getVariable("expireInDays") : 365;
        List<ProcessInstance> processInstances = getAllProcessInstances(execution);
        for(ProcessInstance entry: processInstances) {
            Map<String, Object> processVariables = getVariables(execution, entry);
            String arrivalDate = processVariables.containsKey("citizen_arrival_date") && processVariables.get("citizen_arrival_date") != null ?
                    String.valueOf(processVariables.get("citizen_arrival_date")) : null;
            if(StringUtils.isNotBlank(arrivalDate)) {
                int isolationAge = CalculateIsolationAge(arrivalDate);
                if (isolationAge > expireInDays) {
                    dbdatapipeline.execute(updateVariables(processVariables, execution.getVariables()));
                    execution.getProcessEngineServices().getRuntimeService().deleteProcessInstance(entry.getProcessInstanceId(), "Delete", true, true);
                }
            }
        }
    }

    private List<ProcessInstance> getAllProcessInstances(DelegateExecution execution) {
        List<ProcessInstance> processInstances =
                execution.getProcessEngineServices().getRuntimeService().createProcessInstanceQuery()
                        .processDefinitionKey("covid_travel_plan")
                        .active()
                        .list();

        return processInstances;
    }

    private Integer CalculateIsolationAge(String arrivalDate) {
        String[] split = arrivalDate.split("-");
        LocalDate arrival = LocalDate.of(Integer.valueOf(split[0]), Integer.valueOf(split[1]), Integer.valueOf(split[2]));
        LocalDate now = LocalDate.now();
        return (int) ChronoUnit.DAYS.between(arrival, now);
    }

    private Map<String, Object> getVariables(DelegateExecution execution, ProcessInstance processInstance) {
        return execution.getProcessEngineServices().getRuntimeService().getVariables(processInstance.getId());
    }

    private Map<String,Object> updateVariables(Map<String, Object> processVariables,Map<String, Object> variables) {
        for(Map.Entry<String,Object> entry: variables.entrySet()) {
            processVariables.put(entry.getKey(),entry.getValue());
        }
        return processVariables;
    }


}
