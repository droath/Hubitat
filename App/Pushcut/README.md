## Pushcut

Create actionable notifications using the [pushcut.io](https://pushcut.io) 
service, which is available for iOS. It's a paid service, but has a 
7-day free trial.

## Installation 

### iPhone

1. First, you'll need to install the Pushcut app on your iOS device. Search 
    for `pushcut` on the App Store.
   
2. Next, you will need to create a Notification in the Pushcut app. You'll only 
   need to fill out the `Name` of the Notification. The rest of the fields are 
   a used as defaults. It's common to fill out both the Notification `Title`, 
   and `Message` fields.
   
3. The Notification actions will be configured in the Hubitat application.

### Hubitat

1. First, install the Pushcut application using the Hubitat Package Manager. 
   If you prefer, you can copy the Apps, and the Driver into Hubitat and 
   install manually.

2. After the installation is completed. You'll need to input your Pushcut API
key. Which you can find in the `Account` section in the iPhone Pushcut App.
   
3. Next, you'll be able to create a new `Noification` child application. 
   Input the `Label`, and then select the `Notification` you created in the 
   iPhone Pushcut app.
   
4. Now, click the `Manage Actions` link. You'll be able to add multiple actions 
   to your Notification. 
   
    - Use the `Add Action` button to add another action. The `Remove Action` 
      button will remove the last action added. 
    - Each action needs a `Label`, `Device` and `Command` set.
   
   If the Notification actions only need to be shown based on if a certain 
   condition is met, at the moment we only support `Modes`, and `HSM Status`.
   We're using an `AND` conjunction between all conditions.
   
   Next, if you prefer to use the Hubitat Local API instead of the cloud. 
   Turn on the `Local API` toggle. Also, you can set how often the Access 
   Token should be refreshed. If you added sensitive actions, such as unlocking 
   a door, etc. Then I would recommend setting it to an hour.
   
5. Click, `Next`. Then click `Done`

6. You should now have an `Notification` device created by the `Pushcut` app,
   using the same `Label` you inputted. The device can be used in Rule 
   Machine, WebCore, etc.