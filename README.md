# Wolfpak2
This is the repository for the official Wolfpak app.
## Tips for Testing and Debugging
### User IDs
You can test the app with different (hard-coded) user IDs by editing this line in the constructor of UserIdManager.
```java
mDeviceId = "temp_test_id"; // Replace with "user_id" or any other invalid ID
```
This is useful when testing the main feed, because normally when you vote as a valid user, the posts no longer show up on the feed. By using an invalid user ID, the like status request to the server will fail, meaning the server won't know that the user has already seen the post and will thus show it again in the main feed.
### Location
Currently, the MainFeedFragment and LeaderboardFragment use hard-coded latitude and longitude values. Until the LocationProvider class is implemented satisfactorily, it is recommended to use these hard-coded values.
### Service Providers
In order to access the location, retrieve the user ID, and make requests to the server, you have to use the WolfpakServiceProvider class to obtain a ServiceManager instance. For example, if you want to obtain an instance of the ServerRestClient:
```java
ServerRestClient mClient = (ServerRestClient) WolfpakServiceProvider.getServiceManager(WolfpakServiceProvider.SERVERRESTCLIENT);
```
Note that the return value of _getServiceManager()_ is being casted to the specific class.
