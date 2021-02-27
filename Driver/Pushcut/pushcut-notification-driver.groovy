/** For development only. Do not copy to Hubitat. */

import com.hubitat.app.DeviceWrapper
import com.hubitat.hub.executor.DeviceExecutor
import groovy.transform.Field
import groovy.transform.BaseScript

@Field DeviceWrapper motionSensor
@BaseScript DeviceExecutor deviceExecutor

/**
 *  Pushcut Notification Driver
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

metadata {
    definition(name: "Pushcut Notification Driver", namespace: "droath", author: "Travis") {
        capability "Notification"

        command "sendNotification"

        preferences {
            input(
                name: "logEnable",
                type: "bool",
                title: "Enable debug logging",
                defaultValue: true
            )
        }
    }
}

void parse() {}

void updated() {
    logMessage(
        "info", "${device} Updated!"
    )
}

void installed() {
    logMessage(
        "info", "${device} Installed!"
    )
}

void uninstalled() {
    logMessage(
        "info", "${device} Uninstalled!"
    )
}

void logsOff() {
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

void deviceNotification(String text) {
    try {
        parent?.sendNotification(text)
    } catch(Exception exception) {
        logMessage("error", exception.message)
    }
}

void sendNotification() {
    try {
        parent?.sendNotification()
    } catch(Exception exception) {
        logMessage("error", exception.message)
    }
}

private void logMessage(String type, String message) {
    if (logEnable) {
        switch (type) {
            case "info":
                log.info message
                break
            case "debug":
                log.debug message
                break
            case "error":
                log.error message
                break
        }
    }
}