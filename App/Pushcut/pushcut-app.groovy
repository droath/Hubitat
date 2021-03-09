/**
 *  Pushcut Application
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

import com.hubitat.app.InstalledAppWrapper
import groovyx.net.http.HttpResponseException

definition(
    name: "Pushcut",
    namespace: "droath",
    author: "Travis",
    singleInstance: true,
    description: "Integrate Hubitat with the Pushcut service.",
    category: "Utility",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    String installationState = app.getInstallationState()

    dynamicPage(name: "mainPage", title: "Pushcut", install: true, uninstall: true, submitOnChange: true) {
        if (installationState == "COMPLETE") {
            section(title: titleFormat("simple", "Pushcut Configuration")) {
                input(
                    name: "apiKey",
                    type: "text",
                    title: "API Key",
                    description: "Input the Pushcut API key.",
                    required: true,
                    submitOnChange: true
                )
            }

            if (apiKey) {
                section(title: titleFormat("simple", "Pushcut Notifications")) {
                    app(
                        name: "notificationApp",
                        namespace: "droath",
                        appName: "Pushcut Notification",
                        title: "Create Notification",
                        multiple: true
                    )
                }
            }
            section(title: titleFormat("simple", "Pushcut Debug")) {
                input(
                    name: "logEnable",
                    type: "bool",
                    title: "Enable log messages",
                    defaultValue: true
                )
            }
        } else {
            section() {
                paragraph(
                    "Install the ${app.name} application by clicking Done. After installation is successfully " +
                        "completed revisit the application to configure."
                )
            }
        }
    }
}

void updated() {
    initialized()
}

void installed() {
    initialized()
}

void uninstalled() {
    List<InstalledAppWrapper> childApps = app.getAllChildApps()

    childApps.each {
        childApp -> app.deleteChildApp(childApp.getId())
    }
}

void initialized() {}

HashMap<String, String> getNotificationOptions() {
    HashMap<String, String> options = [:]

    getNotifications().each { notification ->
        String notificationId = notification.id
        String notificationTitle = notification.title
        options.put(notificationId, notificationTitle)
    }

    return options
}

ArrayList getNotifications() {
    return httpGetRequest("notifications")
}

def httpGetRequest(String path) {
    try {
        Map params = [
            uri: "https://api.pushcut.io/v1/",
            path: path,
            contentType: "application/json",
            headers: [
                "API-KEY": apiKey.trim()
            ]
        ]

        httpGet(params) { resp ->
            if (resp.isSuccess()) {
                return resp.getData()
            }
        }
    } catch (Exception exception) {
        log.error(exception.message)
    }
}

def httpPostRequest(String path, Map body = [:]) {
    try {
        Map params = [
            uri: "https://api.pushcut.io/v1/",
            path: path,
            contentType: "application/json",
            headers: [
                "API-KEY": apiKey.trim()
            ],
            body: body
        ]
        httpPostJson(params) { resp ->
            if (resp.isSuccess()) {
                logMessage(
                    "info",
                    "Success: Sent HTTP request!"
                )
            }
        }
    }
    catch (HttpResponseException httpException) {
        String message = httpException.getResponse().getData().error
        logMessage("error", message)
    } catch (Exception exception) {
        logMessage("error", exception.message)
    }
}

String titleFormat(String type, String title) {
    switch (type) {
        case "simple":
            return "<div style=\"background-color: #81BC00; padding: 5px; color: #fff;\">${title}</div>"
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