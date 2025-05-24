<p align="center">
  <img src="https://github.com/user-attachments/assets/b54b55a1-e013-49e9-a059-fddf3473ed6e" alt="vibra-logo" width="400"/>
</p>


## Features

- User authentication (Register/Login)
- User profiles with bios, photos, and posts
- Like and comment on posts
- Real-time chat messaging (WebSocket)
- Friend requests and notifications
- Theme selection (dark, light, etc.)
- Media upload support for posts

---

##  Tech Stack

- **Frontend**: JavaFX
- **Backend/Logic**: Java (JDBC)
- **Database**: MySQL 
- **WebSocket Messaging**: Tyrus
---

##  Prerequisites

Make sure you have the following installed:

- [Java JDK 17+](https://adoptium.net/)
- [JavaFX SDK](https://gluonhq.com/products/javafx/) (ensure it's configured in your IDE)
- [MySQL](https://www.mysql.com/) or SQLite (depending on your DB)
- [Tyrus-WebSocket] Provided in "websocket files" folder

## Setup

Setup
Open the project folder in your preferred Java IDE (IntelliJ IDEA, Eclipse, etc.).

Add the required JAR files to your project’s library/module dependencies. These include JavaFX SDK, SendGrid, Tyrus WebSocket client, and the JDBC driver.

Configure the VM options for JavaFX by adding the following (adjust the path as needed):

css
Copy
Edit
--module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml
Set up the database:

Create the database and tables using the provided schema.sql file.

Update the database connection details (URL, username, password) in the configuration class.

Configure the SendGrid API key:

Obtain an API key from SendGrid.

Add the key to your project configuration or set it as an environment variable.

Run the main application class (Main.java) to launch the app.

