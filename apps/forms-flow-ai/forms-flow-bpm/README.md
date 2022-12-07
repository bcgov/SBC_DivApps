# Formsflow Documentation

The following documentation gives an account of all the formsflow files present in this repository.

## **Table of Contents**
1. [forms-flow-web](#forms-flow-web)
    * [Required files](#required-files)
    * [Current structure](#current-structure)
    * [Files to be removed](#files-to-be-removed)
2. [forms-flow-web](#forms-flow-web)
    * [Required files](#required-files)
    * [Current structure](#current-structure)
    * [Files to be removed](#files-to-be-removed)
3. [Guidelines for formsflow upgrades](#guidelines-for-formsflow-upgrades)
4. [How to debug?](#how-to-debug)
    * [Data has not been written to analytics](#data-has-not-been-written-to-analytics)
    * [Data has been written to analytics](#data-has-been-written-to-analytics)
5. [Logging](#logging)

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
