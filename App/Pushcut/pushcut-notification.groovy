/** For development only. Do not copy to Hubitat. */

import com.hubitat.app.Device
import com.hubitat.hub.executor.DeviceExecutor
import groovy.transform.Field
import groovy.transform.BaseScript

@Field DeviceWrapper motionSensor
@BaseScript DeviceExecutor deviceExecutor

/**
 *  Pushcut Notification
 *
 *  Copyright 2021
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 *
 *    Date        Who             	What
 *    ----        ---             	----
 *    2021-1-31  Travis   	Original Creation
 */

import com.hubitat.app.DeviceWrapper
import com.hubitat.app.ChildDeviceWrapper

definition(
    name: "Pushcut Notification",
    namespace: "droath",
    parent: "droath:Pushcut",
    oauth: true,
    author: "Travis",
    description: "The child app for notification in the pushcut application.",
    category: "Utility",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

mappings {
    path("/devices/:id/:command"){
        action: [
            GET: "deviceCommandHandler"
        ]
    }
}

preferences {
    page(name: "notificationConfig")
    page(name: "notificationAction")
}

def notificationConfig() {
    dynamicPage(name: "notificationConfig", title: "Pushcut Notification", install: true, uninstall: true) {
        section(title: titleFormat("simple", "General")) {
            input(
                name: "label",
                type: "text",
                title: "Label",
                required: true,
                defaultValue: app.getLabel(),
                submitOnChange: true
            )
        }
        section(title: titleFormat("simple", "Configuration")) {
            input(
                name: "name",
                type: "enum",
                required: true,
                title: "Notification",
                description: "Select the Pushcut notification.",
                options: parent.getNotificationOptions()
            )
            paragraph("Add actions that will be merged or added to the notification.")
            href(
                title: "Manage Actions",
                page: "notificationAction"
            )
        }
    }
}

def notificationAction() {
    dynamicPage(name: "notificationAction", title: "Notification Actions") {
        section() {
            paragraph("Add the actions that would be selectable in the notification.")
        }
        int actionCount = getActionCount()

        if (actionCount >= 1) {
            for (int index = 0; index < actionCount; index++) {
                section(title: "Action ${index + 1}", hideable: true, hidden: (actionCount - 1) != index) {
                    actionInputs(index)
                }
            }
        }
        else {
            section {
                paragraph("There are no notification actions defined.")
            }
        }

        section() {
            input(name: "addAction", type: "button", title: "Add Action", submitOnChange: true)

            if (actionCount >= 1) {
                input(name: "removeAction", type: "button", title: "Remove Action", submitOnChange: true)
            }
        }

        if (actionCount >= 1) {
            section(title: titleFormat("simple", "Actions Conditions")) {
                paragraph(
                    "Configure if the actions should be restricted. The actions will only show up when the " +
                        "conditions are met."
                )
                input(
                    name: "actionModes",
                    type: "mode",
                    title: "Select Modes",
                    multiple: true
                )
                input(
                    name: "actionHsmStatus",
                    type: "enum",
                    title: "Select HSM Status",
                    options: getHsmStatusOptions()
                )
            }

            section(title: titleFormat("simple", "Actions API Configurations")) {
                paragraph("Enable if you want to use the local API instead of the cloud.")
                input(
                    name: "useLocalAPI",
                    type: "bool",
                    title: "Local API",
                )

                paragraph("Select how many hours to wait until refreshing the access token.")
                input (
                    name: "actionRefreshTokenHour",
                    type: "enum",
                    title: "Refresh Access Token",
                    required: true,
                    options: 1..5,
                    defaultValue: 1,
                )

                if (state.accessToken) {
                    input(
                        name: "resetAccessToken",
                        type: "button",
                        title: "Reset Access Token",
                        submitOnChange: true,
                    )
                }
            }
        }
    }
}

def actionInputs(int index) {
    input(
        name: "action.${index}.label",
        type: "text",
        title: "Label",
        required: true
    )
    String deviceName = "action.${index}.device"

    input(
        name: deviceName,
        type: "capability.*",
        title: "Device",
        required: true,
        submitOnChange: true,
    )

    if (settings[deviceName]) {
        DeviceWrapper device = settings[deviceName]
        List options = device.getSupportedCommands()*.name.sort()

        input(
            name: "action.${index}.command",
            type: "enum",
            title: "Command",
            required: true,
            options: options
        )
    }
}

void updated() {
    initialized()
    deleteOldActions()

    if (getActionCount() == 0) {
        [
            "actionModes",
            "useLocalAPI",
            "actionHsmStatus",
        ].each {settingName ->
            app.removeSetting(settingName)
        }
    }
}

void installed() {
    initialized()
}

void uninstalled() {
    app.deleteChildDevice(
        getNotificationDevice().getDeviceNetworkId()
    )
}

void initialized() {
    if (label) {
        app.updateLabel(label)
        app.removeSetting("label")
    }

    if (!hasNotificationDevice()) {
        try {
            app.addChildDevice(
                "droath",
                "Pushcut Notification Driver",
                generateNotificationDeviceId(),             [
                "name" : "Pushcut Notification Driver",
                "label": label
            ]
            )
        } catch (Exception exception) {
           log.error(exception.getMessage())
        }
    }

    if(!state.accessToken) {
        createAccessToken()
    }

    if (actionRefreshTokenHour) {
        schedule(
            "0 0 */${actionRefreshTokenHour} ? * *",
            "resetAccessToken"
        )
    }
}

String titleFormat(String type, String title) {
    return parent?.titleFormat(type, title)
}

void deviceCommandHandler() {
    try {
        if (!params["id"] || !params["command"]) {
            throw new RuntimeException(
                "Missing id and/or command parameters!"
            )
        }
        String paramsId = params["id"]
        String paramsCommand = params["command"]

        Integer deviceId = Integer.parseInt(paramsId)
        callActionDeviceCommand(deviceId, paramsCommand)
    } catch (Exception exception) {
        log.error(exception.getMessage())
    }
}

void appButtonHandler(String name) {
    switch(name) {
        case "addAction":
            addAction()
            break
        case "removeAction":
            removeAction()
            break
        case "resetAccessToken":
            resetAccessToken()
            break
    }
}

void addAction() {
    state.actionCount++;
}

void removeAction() {
    state.actionCount--;
}

int getActionCount() {
    if (!state.actionCount) {
        state.actionCount = 0
    }

    return (int) state.actionCount
}

List getActionModes() {
    return settings?.actionModes ?: []
}

void deleteOldActions() {
    int nextActionIndex = getActionCount()
    boolean oldActionFound = true

    while(oldActionFound) {
        oldActionFound = false

        if (settings["action.${nextActionIndex}.label"]) {
            ["label", "device", "command"].each { attribute ->
                app.removeSetting("action.${nextActionIndex}.${attribute}")
            }
            nextActionIndex++
            oldActionFound = true
        }
    }
}

Boolean hasActionConditionBeenMet() {
    List<String> modes = getActionModes()

    if (modes.isEmpty() && !actionHsmStatus) {
        return true
    }
    List verdicts = []

    if (!modes.isEmpty()) {
        verdicts += (boolean) modes.contains(location.mode)
    }

    if (actionHsmStatus) {
        verdicts += ((boolean) (location.hsmStatus == actionHsmStatus))
    }
    verdicts.unique()

    if (verdicts.size() == 2) {
        return false
    }

    return (boolean) verdicts.first()
}

void sendNotification(String text = null) {
    Map body = [:]
    List actionsPayload = getActionsPayload()

    if (text) {
        body["text"] = text
    }

    if (!actionsPayload.isEmpty()) {
        body["actions"] = actionsPayload
    }

    parent?.httpPostRequest("notifications/${name}", body)
}

List getActionsPayload() {
    List actionsPayload = []

    if (hasActionConditionBeenMet()) {
        getActionDefinitions().each { definition ->
            if (definition.label && definition.device && definition.command) {
                String name = definition.label
                String command = definition.command

                DeviceWrapper device = definition.device
                Integer deviceId = device.getId().toInteger()

                actionsPayload.push(
                    buildActionUrlPayload(
                        name,
                        buildDeviceCommandUrl(deviceId, command)
                    )
                )
            }
        }
    }

    return actionsPayload
}

String buildDeviceCommandUrl(Integer deviceId, String command) {
    String urlBase = getFullApiServerUrl()

    if (useLocalAPI) {
        urlBase = getFullLocalApiServerUrl()
    }

    return "${urlBase}/devices/${deviceId}/${command}?access_token=${state?.accessToken}"
}

void callActionDeviceCommand(Integer deviceID, String command) {
    DeviceWrapper device = findActionDevice(deviceID)

    if (!device.hasCommand(command)) {
        throw new RuntimeException(
            sprintf(
                "Unable to locate the %s command for the %s device.",
                [ command, device.getDisplayName()]
            )
        )
    }

    device."${command}"()
}

DeviceWrapper findActionDevice(Integer deviceId) {
    return getActionDevices().find {it ->
        it.getKey() == deviceId
    }.getValue()
}

Map<Integer, String> getActionDevices() {
    Map<Integer,String> actionDevices = [:]

    getActionDefinitions().each { definition ->
        if (definition.device) {
            DeviceWrapper device = definition.device
            Integer deviceId = device.getId().toInteger()

            if (!actionDevices.containsKey(deviceId)) {
                actionDevices.put(deviceId, device)
            }
        }
    }

    return actionDevices
}

List getActionDefinitions() {
    List definitions = []

    for (int index = 0; index <= (getActionCount() - 1); index++) {
        HashMap<String,String> definition = new HashMap()

        ["label", "device", "command"].each {attribute ->
            String attributeName = "action.${index}.${attribute}"

            if (settings[attributeName]) {
                definition.put(
                    attribute, settings[attributeName]
                )
            }
        }

        if (definition.isEmpty() || definition.size() != 3) {
            continue
        }

        definitions.push(definition)
    }

    return definitions
}

Map buildActionUrlPayload(String name, String url) {
    return [
        name: name,
        url: url,
        urlBackgroundOptions: [
            httpMethod: "GET",
            httpContentType: "application/json"
        ]
    ]
}

def getNotificationDevice() {
    List<ChildDeviceWrapper> devices = app.getChildDevices()

    if (devices.isEmpty()) {
        return null
    }

    return devices.first()
}

Map getHsmStatusOptions() {
    return [
        "disarmed": "Disarmed",
        "armedAway": "Armed Away",
        "armingAway": "Arming Away",
        "armedHome": "Armed Home",
        "armingHome": "Arming Home",
        "armedNight": "Armed Night",
        "armingNight": "Arming Night",
        "allDisarmed": "All Disarmed",
    ]
}

def resetAccessToken() {
    revokeAccessToken()
    createAccessToken()
}

Boolean hasNotificationDevice() {
    return getNotificationDevice() instanceof ChildDeviceWrapper
}

String generateNotificationDeviceId() {
    return "pushcut:notification:${UUID.randomUUID().toString()}"
}