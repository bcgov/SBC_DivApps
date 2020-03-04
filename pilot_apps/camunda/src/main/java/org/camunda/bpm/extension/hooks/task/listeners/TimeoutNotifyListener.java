package org.camunda.bpm.extension.hooks.task.listeners;

import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.*;
import org.camunda.bpm.extension.hooks.services.IMessageEvent;
import org.joda.time.DateTime;

/**
 * Timeout Task Listener to start a message event when the deadline is due
 *
 * @author yichun.zhao@aot-technologies.com
 */
public class TimeoutNotifyListener implements TaskListener, IMessageEvent {

    private Expression remind;
    private Expression escalate;

    private static final Logger log = Logger.getLogger(TimeoutNotifyListener.class.getName());

    /**
     * This calculates time logic
     * and provides the necessary information to send message.
     *
     * @param delegateTask: The task which sends the message
     */
    public void notify(DelegateTask delegateTask) {

        if (StringUtils.isNotEmpty(String.valueOf(delegateTask.getVariable("assigned_date")))) {

            DateTime currentDate = new DateTime();
            DateTime assignedDate = new DateTime(String.valueOf(delegateTask.getVariable("assigned_date")));
            DateTime remindDate = getCalculatedDate(String.valueOf(this.remind.getValue(delegateTask)),assignedDate);
            DateTime escalateDate = getCalculatedDate(String.valueOf(this.escalate.getValue(delegateTask)),assignedDate);
            DateTime stopNotifyDate = getCalculatedDate("1m",escalateDate); this.addMins(escalateDate, 1);

            // Check if escalate first because reminder date is before escalation date
            if ((currentDate.isAfter(escalateDate) && currentDate.isBefore(stopNotifyDate))) {
                sendMessage(delegateTask,"activity_escalation");
            } else if ((currentDate.isAfter(remindDate) && currentDate.isBefore(stopNotifyDate))) {
                sendMessage(delegateTask,"activity_reminder");
            }
        }
    }

    /**
     * This adds days to a Date object.
     *
     * @param date: The date to be changed
     * @param days: The number of days to be added
     */
    private DateTime addDays(DateTime date, int days) {
        return date.plusDays(days);
    }

    // This is just for testing
    private DateTime addMins(DateTime date, int mins) {
        return date.plusMinutes(mins);
    }


    private DateTime getCalculatedDate(String input, DateTime date) {
        if(StringUtils.isNotEmpty(input)) {
            String pattern = StringUtils.endsWithIgnoreCase(input, "m") ? "m" :
                    StringUtils.endsWithIgnoreCase(input, "d") ? "d" :"x";
            String numStr = StringUtils.endsWithIgnoreCase(input, "m") ? StringUtils.substringBefore(input.toLowerCase(), "m") :
                    StringUtils.endsWithIgnoreCase(input, "d") ? StringUtils.substringBefore(input.toLowerCase(), "d") : null;
            int value = StringUtils.isNotEmpty(numStr) ? Integer.parseInt(numStr) : 0;
            if ("m".equals(pattern)) {
                return this.addMins(date, value);
            } else if ("d".equals(pattern)) {
                return this.addDays(date, value);
            } else {
                log.info("Invalid input. Resending input");
            }
        }
        return date;
    }

}
