package org.camunda.bpm.extension.hooks.services;

import freemarker.template.Template;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.impl.context.Context;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;

import freemarker.template.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interface containing method to send email
 *
 * @author yichun.zhao@aot-technologies.com
 */
public interface IEmail {

    static final Logger log = Logger.getLogger(IEmail.class.getName());

    /**
     * This sends out the message to start a message event.
     *
     * @param delegateTask: The task which sends the message
     * @param category: Category of email: notification/reminder/escalation
     */
    default void sendEmail(DelegateTask delegateTask, String category) throws IOException {
        FileInputStream propFile = null;
        Properties prop = new Properties();
        try {
            propFile = new FileInputStream("./mail-config.properties");
            prop.load(propFile);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (propFile != null) {
                    propFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        // Get user profile
        IdentityService identityService = Context.getProcessEngineConfiguration().getIdentityService();
        User user = identityService.createUserQuery().userId(delegateTask.getAssignee()).singleResult();

        if (user != null) {
            // TODO: Get cc user emails

            Email email = new SimpleEmail();
            email.setSmtpPort(Integer.parseInt(prop.getProperty("mail.smtp.port")));
            email.setHostName(prop.getProperty("mail.smtp.host"));
            email.setAuthentication(prop.getProperty("mail.user"), prop.getProperty("mail.password"));
            email.setStartTLSEnabled(true);

            // Use FTL
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
            cfg.setClassForTemplateLoading(IEmail.class, "/templates/");
            Writer emailBody = new StringWriter();
            Map<String, Object> input = new HashMap<>();
            input.put("taskid", delegateTask.getId());
            input.put("firstname", user.getFirstName());
            input.put("lastname", user.getLastName());

            try {
                email.setFrom(prop.getProperty("mail.user"));
                if (category.equals("assignment_notification")) {
                    email.setSubject("CCII Task Notification");

                    Template template = cfg.getTemplate("notification.ftl");
                    template.process(input, emailBody);
                    email.setMsg(emailBody.toString());

                } else if (category.equals("activity_reminder")) {
                    email.setSubject("CCII Task Reminder");

                    Template template = cfg.getTemplate("reminder.ftl");
                    template.process(input, emailBody);
                    email.setMsg(emailBody.toString());

                } else if (category.equals("activity_escalation")) {
                    email.setSubject("CCII Task Escalation");

                    Template template = cfg.getTemplate("escalation.ftl");
                    template.process(input, emailBody);
                    email.setMsg(emailBody.toString());

                }
                email.addTo(user.getEmail());
                email.send();
                log.info("Email successfully sent to address '" + user.getEmail() + "'.");

            } catch (Exception e) {
                log.log(Level.WARNING, "Could not send email to assignee", e);
            }

            /* Send message to start a message event
            RuntimeService runtimeService = delegateTask.getProcessEngineServices().getRuntimeService();
            runtimeService.createMessageCorrelation("Message_Notify")
                    .setVariable("task_id", delegateTask.getId())
                    .setVariable("to_email", user.getEmail())
                    .setVariable("first_name", user.getFirstName())
                    .setVariable("last_name", user.getLastName())
                    .setVariable("cc_emails", null)
                    .setVariable("category", category)
                    .correlateStartMessage();
            log.info("\n\nMessage sent! " + "\n\n");*/
        }
    }
}
