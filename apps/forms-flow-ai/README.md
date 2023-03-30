# Formsflow Documentation

The following documentation gives an account of all the formsflow files present in this repository.

## **Table of Contents**
1. [forms-flow-web](#forms-flow-web)
    * [Required files](#required-files)
    * [Current structure](#current-structure)
    * [Files to be removed](#files-to-be-removed)
2. [forms-flow-bpm](#forms-flow-bpm)
    * [Required files](#required-files-1)
    * [Current structure](#current-structure-1)
    * [File catalog](#forms-flow-bpm-files-catalog)
    * [Listeners readme](#forms-flow-bpm-listeners)
3. [Guidelines for formsflow upgrades](#guidelines-for-formsflow-upgrades)
    * [Listeners readme](#most-diverged-files)
4. [Serviceflow in the Q](#serviceflow-in-the-q)
    * [Files in the q](#files-in-the-q)

---

## **forms-flow-web**

When modifying these files to realign with the latest formsflow, care must be taken to ensure that the look and feel of the application is not affected.

### **Required files:**
- `src/components/ServiceFlow/filter/ServiceTaskFilterListDropDown.js`
    - Uses different css classes from open source because, the navigation bar look and feel is different
- `src/containers/NavBar.jsx`
    - Uses different css classes from open source.
    - Should be the frequently verified for changes in open source and reoriented.
- `src/containers/styles.scss` and `src/styles.scss`
    - Many common css classes from open source; numerous different css classes needed for service BC.
    - Should be the frequently verified for changes in open source and reoriented.
- `public/webfonts/fa-regular_list-alt.svg` and `public/webfonts/fa-solid_list.svg`
    - These two fontawesome icons are used in the navigation bar.
    - Can be removed if suitable css classes are identified in place of these icons.
- `public/logo.svg`
    - This is BC government logo that is used in Serviceflow.
- `Dockerfile`
    - The `Dockerfile` pulls in the open source repository contents and replaces the files in open source with ones in this repository (only those that exist).  

### **Files to be removed:**

The following files are slated to be removed because we can leverage the open source version of these files. These files were added for debugging purposes or testing new features which have since been adopted into `formsflow` open source.

| File | Reason for adding |
| ----------- | ----------- |
| `src/apiManager/services/bpmTaskServices.js` | Added to test a new feature (now available in open source) |

### Current structure:
```
├──📂src
│   ├──📂components/ServiceFlow/filter
│   │   ├──📜ServiceTaskFilterListDropDown.js
│   ├── containers
|   |   ├──📜NavBar.jsx
|   |   ├──📜styles.scss
│   ├──📜styles.scss
├──📂public
│   ├── webfonts/*.svg
│   ├── logo.svg
```
	 
## **forms-flow-bpm**

### **Required files:**

- `pom-docker.xml`
    - Needed for redis dependency; otherwise, the dependencies are largely identical to open source.
- `src/main/resources/application.yaml`
    - Useful to have application.yaml in this repo as it is used for customizing camunda.
    - Should be the frequently verified for changes in open source and reoriented.
- `test/*`
    - Some open source test cases will fail because of Service BC customizations.
    - It is important to identify those test cases and modify or remove them as applicable.
    - Test failures can be identified by build failures.
- `src/main/java/org/camunda/bpm/extension/`
    - All of them are required customizations for service BC.
    - Some files are common with open source but they need to be here because of redis configurations used for websocket.

### Current structure:

```
📦src
 ┣ 📂main
 ┃ ┣ 📂java
 ┃ ┃ ┗ 📂org.camunda.bpm.extension
 ┃ ┃ ┃ ┣ 📂commons
 ┃ ┃ ┃ ┃ ┣ 📂connector.support
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜ApplicationAccessHandler.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📂io.event
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜CamundaEventListener.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜TaskEventTopicListener.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📂socket.service
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜TaskEventMessageService.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜RedisConfig.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜WebSocketConfig.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜ITaskEvent.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📂hooks
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📂controllers
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜DataReaderController.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜FormBuilderPipelineController.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📂listeners
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📂execution
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜ActiveDirectoryListener.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜AutoCloseListener.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜ExternalSubmissionListener.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜GroupAttributesListener.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜GroupNotifyListener.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📂task
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜AccessGrantNotifyListener.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜FormConnectorListener.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜AnalyticsListener.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜BaseListener.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜BPMFormDataPipelineListener.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📂services
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📂analytics
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜AbstractDataPipeline.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜DataPipelineResponse.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜IDataPipeline.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜IQueryFactory.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜SimpleDBDataPipeline.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜FormSubmissionService.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜IMessageEvent.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜IUser.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📂keycloak
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📂rest
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📂client
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜WebClientOauth2Config.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜FormBuilderPipelineSecurityConfig.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜RestApiSecurityConfig.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜CamundaExtConfiguration.java
 ┃ ┗ 📂resources
 ┃ ┃ ┗ 📜application.yaml
 ┗ 📂test
 ┃ ┗ 📂java
 ┃ ┃ ┗ 📂org.camunda.bpm.extension.commons
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📂connector.support
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜ApplicationAccessHandlerTest.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📂io.event
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜CamundaEventListenerTest.java

```

### forms-flow-bpm files catalog

| File | Reason why it exists | Can it be removed? |
| ----------- | ----------- | ----------- |
| ApplicationAccessHandler | The authentication in formsflow open source changed between 4.0.3 and 4.0.4 releases with breaking changes. Hence this class exists to support the older way of authentication. | Yes with authentication upgrades, otherwise no. |
| CamundaEventListener  | Needed for brokering connection between BPM pods; uses redis | No |
| TaskEventTopicListener  | Needed for brokering connection between BPM pods; uses redis | No |
| TaskEventMessageService  | Needed for brokering connection between BPM pods; uses redis | No |
| RedisConfig  | Needed for brokering connection between BPM pods; uses redis | No |
| WebSocketConfig  | Needed for brokering connection between BPM pods; uses redis | No |
| ITaskEvent  | Needed for brokering connection between BPM pods; uses redis | No |
| DataReaderController  | To be deleted. Previously useful for querying the analytics database | Yes |
| FormBuilderPipelineController  | Needed for capturing data from orbeon forms for customer feedback | No |
| FormConnectorListener  | Captures data from orbeon forms for customer feedback | No |
| application.yaml  | Needed for specifying custom config like `authenticationGrantType` used for authentication | No |

### forms-flow-bpm Listeners

   Name | Type | How it Works |
 --- | --- | --- |
 | `AutoCloseListener`| Execution Listener |This component can be used on any event of execution listener. It takes a SELECT query as input and closes the process instances which result from the select query.|
| `AnalyticsListener`| Task/Execution Listener |This component can be used on any event of task/execution listener. This is used to commit task data to the analytics database.|
| `ActiveDirectoryListener`| Execution Listener |This component can be used on any event of task/execution listener. It is used for searching a user on ldap.|
 |`GroupAttributesListener`| Execution Listener |This component can be used on any event of execution listener. It is used to inject the region and group attributes corresponding to a task.|
 |`GroupNotifyListener (currently not used)`| Execution Listener |This component can be used on a **CREATE** event of execution listener. It is used to send emails when a task is created to additional groups of interest.|
 |`FormConnectorListener`| Task Listener |This component can be used on **CREATE** event of task listener. This serves to associate a form with task.|
| `ExternalSubmissionListener`| Execution Listener |This component allows direct integration from any external system and does offline sync-up within formsflow.ai i.e creates submission in formio.|
 |`BPMFormDataPipelineListener`| Task/Execution Listener |This component can be used on any event of task/execution listener. It is used for populating CAM Variables into formio data.|

 ### Listeners from the open source:

 [listeners-readme.md on forms-flow-ai](https://github.com/AOT-Technologies/forms-flow-ai/blob/master/forms-flow-bpm/starter-examples/listeners/listeners-readme.md)
          

## **Guidelines for formsflow upgrades**

Upon a new release of open source, care should be taken to keep all the files aligned with open source without removing the customizations. The best way to deal with merges is by using the git blame tool to identify the commit history and changes prior to that and use a diff checker tool narrow down the scope for changes.

### **Most diverged files**
*forms-flow-web*
- All files under `src` folder.

*forms-flow-bpm*
- `pom-docker.xml`
    - Java dependencies' versions may be different.
- `application.yaml`
    - should be checked for updates as well but not all changes are applicable to service BC. Changes should be done on a case by case basis based on Changelog documentation.
- `*src/*.java` 
    - The customized files generally do not need any updates during formsflow upgrades since those files do not exist in open source. However, dependency upgrades may cause compilation or runtime errors.
    - Those files that do exist in open source should be merged correctly upon a new formsflow release.

## **Serviceflow in the Q**

Serviceflow in The Q is a vue component that is integrated with the queue-management system through the frontend component. It makes use of the 
[Formsflow Vue Component](https://github.com/AOT-Technologies/forms-flow-ai-extensions/tree/master/camunda-formio-tasklist-vue) that is developed by AOT Technologies.

In order to make changes, raise a PR to the queue-management git from a fork. Every change to the vue component will have to be released in order to properly be absorbed into the queue system.

### Files in the q

- `package.json`: Pulls in the `camunda-formio-tasklist-vue` dependency.
- `package-lock.json`: Execute `npm install` and commit the changes to `package-lock.json` too.
- `frontend/src/views/Tasklist.vue`: The vue component that adds the formsflow vue component into the queue frontend.
- `frontend/src/assets/css/service-flow.css`: Contains the css.
- `frontend/src/views/FormView.vue`: Pulls in the forms set up in formsflow.

**Less frequently used files**
- `frontend/src/MainApp.vue`: Some css styles
- `frontend/src/components/ServeCitizen/serve-citizen.vue`: Some bug fixes formsflow needs
