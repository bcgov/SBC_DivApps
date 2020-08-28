package org.camunda.bpm.extension.hooks.task.listeners;

import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.StringValue;
import org.camunda.bpm.extension.hooks.services.IMessageEvent;
import org.camunda.bpm.extension.hooks.services.analytics.IDataPipeline;
import org.camunda.bpm.extension.hooks.services.analytics.SimpleDBDataPipeline;
import org.glassfish.jersey.internal.util.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Named;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class has been written specific to process, and to be enhanced to give generic behavior.
 *
 * @author sumathi.thirumani@aot-technologies.com
 */
@Named("autocloseDelegate")
public class AutoCloseListener implements ExecutionListener , IMessageEvent {

    @Autowired
    private SimpleDBDataPipeline dbdatapipeline;

    private final Logger LOGGER = Logger.getLogger(AutoCloseListener.class.getName());

    @Override
    public void notify(DelegateExecution execution) {
        Integer expireInDays = execution.getVariables().containsKey("expireInDays") ? (Integer) execution.getVariable("expireInDays") : 365;
        List<ProcessInstance> processInstances = getAllProcessInstances(execution);
        List<String> deletedInstances = new ArrayList<>();
        LOGGER.info("Begin - AutoClose job");
        for(ProcessInstance entry: processInstances) {
            Map<String, Object> processVariables = getVariables(execution, entry);
            String arrivalDate = processVariables.containsKey("citizen_arrival_date") && processVariables.get("citizen_arrival_date") != null ?
                    String.valueOf(processVariables.get("citizen_arrival_date")) : null;
            //Condition#1 : Expire when arrival date exceeds isolation period
            Boolean deleteEntry = Boolean.FALSE;
            if(StringUtils.isNotBlank(arrivalDate) && CalculateIsolationAge(arrivalDate) > expireInDays) {
                    deleteEntry = Boolean.TRUE;
            }
            //Condition#2 : Age is under 12.
            String dob = processVariables.containsKey("citizen_birth_date") && processVariables.get("citizen_birth_date") != null ?
                    String.valueOf(processVariables.get("citizen_birth_date")) : null;
            if(StringUtils.isNotBlank(dob) && CalculateAge(dob) < 12) {
                    deleteEntry = Boolean.TRUE;
            }
            if(deleteEntry) {
                try {
                    Map<String,Object> rspVariableMap = dbdatapipeline.execute(updateVariables(processVariables, execution.getVariables()));
                    execution.getProcessEngineServices().getRuntimeService().deleteProcessInstance(entry.getProcessInstanceId(), "Delete", true, true);
                    deletedInstances.add(entry.getProcessInstanceId());
                    notifyForAttention(execution, entry.getProcessInstanceId(), rspVariableMap);
                }catch(Exception ex) {
                    LOGGER.log(Level.SEVERE, "Exception occurred while closing pid:"+entry.getProcessInstanceId(), ex);
                    notifyForAttention(execution, entry.getProcessInstanceId(), ExceptionUtils.exceptionStackTraceAsString(ex));
                }
                }
            }
            LOGGER.info("End - AutoClose job. Count ="+deletedInstances.size());
            LOGGER.info("End - AutoClose job. Deleted Instances = "+deletedInstances);
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

    private Integer CalculateAge(String dob) {
        String[] split = dob.split("-");
        LocalDate bdate = LocalDate.of(Integer.valueOf(split[0]), Integer.valueOf(split[1]), Integer.valueOf(split[2]));
        LocalDate now = LocalDate.now();
        return Period.between(bdate, now).getYears();
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

    private void notifyForAttention(DelegateExecution execution, String pid, Map<String,Object> rspVariableMap){
        if(IDataPipeline.ResponseStatus.FAILURE.name().equals(rspVariableMap.get("code")) ) {
            for(Map.Entry<String,Object> entry : rspVariableMap.entrySet()) {
                if(StringUtils.startsWith(entry.getKey(),"exception")) {
                    notifyForAttention(execution, pid, String.valueOf(rspVariableMap.get("exception")));
                }
            }
        }
    }

    private void notifyForAttention(DelegateExecution execution, String pid,String exception){
            Map<String,Object> exVarMap = new HashMap<>();
            //Additional Response Fields - BEGIN
            exVarMap.put("pid",pid);
            exVarMap.put("subject","Exception Alert: AutoClose Job");
            exVarMap.put("category","autoclose_service_exception");
            StringValue exceptionDataValue = Variables.stringValue(exception,true);
            exVarMap.put("exception",exceptionDataValue);
            //Additional Response Fields - END
            sendMessage(execution,exVarMap);
            LOGGER.info("\n\nMessage sent! " + "\n\n");

    }


}
