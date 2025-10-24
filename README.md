# Red5 Pro Server Example Plugin

This project serves as a template and example for developers who want to build custom plugins for Red5 Pro Server. It demonstrates the core plugin architecture, stream listener integration, servlet configuration, and best practices for extending Red5 Pro functionality.

## Overview

Red5 Pro plugins allow you to extend the server with custom functionality such as:
- Custom stream processing and manipulation
- HTTP endpoints and REST APIs
- Integration with external services
- Custom authentication and authorization
- Recording and transcoding workflows
- Analytics and monitoring

This example includes:
- **MyRed5ProPlugin**: Main plugin class demonstrating lifecycle management
- **MyProStreamListener**: Stream listener for intercepting and processing media packets
- **MyServlet**: HTTP servlet integration example

## Prerequisites

- **Java 21** or higher
- **Maven 3.6.0** or higher
- **Red5 Pro Server** installation (for deployment and testing)

## Setting Up Red5 Pro Dependencies

Since Red5 Pro's Maven repository (https://red5pro.jfrog.io) requires authentication, you'll need to install the Red5 Pro JARs from your server distribution into your local Maven repository.

### Option 1: Install JARs from Red5 Pro Server Distribution

Navigate to your Red5 Pro Server installation and run these commands to install the required JARs to your local Maven repository:

```bash
# Set your Red5 Pro version
RED5PRO_VERSION=15.0.0
RED5PRO_COMMON_VERSION=14.3.0.5
RED5PRO_NOTIFICATIONS_VERSION=14.3.1
RED5_VERSION=2.0.22

# Navigate to your Red5 Pro Server lib directory
cd /path/to/red5pro/lib

# Install red5pro-internal
mvn install:install-file \
  -Dfile=red5pro-internal-${RED5PRO_VERSION}.jar \
  -DgroupId=com.red5pro \
  -DartifactId=red5pro-internal \
  -Dversion=${RED5PRO_VERSION} \
  -Dpackaging=jar

# Install red5pro-common
mvn install:install-file \
  -Dfile=red5pro-common-${RED5PRO_COMMON_VERSION}.jar \
  -DgroupId=com.red5pro \
  -DartifactId=red5pro-common \
  -Dversion=${RED5PRO_COMMON_VERSION} \
  -Dpackaging=jar

# Install red5pro-notifications
mvn install:install-file \
  -Dfile=red5pro-notifications-${RED5PRO_NOTIFICATIONS_VERSION}.jar \
  -DgroupId=com.red5pro \
  -DartifactId=red5pro-notifications \
  -Dversion=${RED5PRO_NOTIFICATIONS_VERSION} \
  -Dpackaging=jar

# Install red5pro-mega
mvn install:install-file \
  -Dfile=red5pro-mega-${RED5PRO_VERSION}.jar \
  -DgroupId=com.red5pro \
  -DartifactId=red5pro-mega \
  -Dversion=${RED5PRO_VERSION} \
  -Dpackaging=jar

# Install red5pro-restreamer-plugin
mvn install:install-file \
  -Dfile=red5pro-restreamer-plugin-${RED5PRO_VERSION}.jar \
  -DgroupId=com.red5pro \
  -DartifactId=red5pro-restreamer-plugin \
  -Dversion=${RED5PRO_VERSION} \
  -Dpackaging=jar

# Install red5pro-whip-plugin
mvn install:install-file \
  -Dfile=red5pro-whip-plugin-${RED5PRO_VERSION}.jar \
  -DgroupId=com.red5pro \
  -DartifactId=red5pro-whip-plugin \
  -Dversion=${RED5PRO_VERSION} \
  -Dpackaging=jar

# Install Red5 Server dependencies
mvn install:install-file \
  -Dfile=red5-server-${RED5_VERSION}.jar \
  -DgroupId=org.red5 \
  -DartifactId=red5-server \
  -Dversion=${RED5_VERSION} \
  -Dpackaging=jar

mvn install:install-file \
  -Dfile=red5-io-${RED5_VERSION}.jar \
  -DgroupId=org.red5 \
  -DartifactId=red5-io \
  -Dversion=${RED5_VERSION} \
  -Dpackaging=jar

mvn install:install-file \
  -Dfile=red5-server-common-${RED5_VERSION}.jar \
  -DgroupId=org.red5 \
  -DartifactId=red5-server-common \
  -Dversion=${RED5_VERSION} \
  -Dpackaging=jar
```

### Option 2: Remove Private Repository (If You Have Local JARs)

After installing the JARs locally, you can remove or comment out the Red5 Pro JFrog repository in `pom.xml`:

```xml
<!-- Comment out or remove this repository -->
<!--
<repository>
    <id>red5pro-ext-release</id>
    <url>https://red5pro.jfrog.io/red5pro/ext-release-local</url>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
-->
```

### Option 3: Configure Maven Settings with Credentials (If You Have Access)

If you have Red5 Pro repository credentials, add them to `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>red5pro-ext-release</id>
      <username>YOUR_USERNAME</username>
      <password>YOUR_PASSWORD</password>
    </server>
  </servers>
</settings>
```

## Building the Plugin

Once dependencies are configured:

```bash
# Clean and build
mvn clean package

# The output JAR will be in: target/red5pro-example-plugin-1.0.0.jar
```

### Code Formatting

The project uses Red5 Pro's code formatting standards:

```bash
# Format all Java files
mvn formatter:format
```

## Deploying the Plugin

1. **Build the plugin** (see above)

2. **Copy the JAR** to your Red5 Pro Server plugins directory:
   ```bash
   cp target/red5pro-example-plugin-1.0.0.jar /path/to/red5pro/plugins/
   ```

3. **Restart Red5 Pro Server**:
   ```bash
   cd /path/to/red5pro
   ./red5-shutdown.sh
   ./red5.sh
   ```

4. **Verify the plugin loaded** by checking the logs:
   ```bash
   tail -f /path/to/red5pro/log/red5.log
   ```

   You should see: `Starting MyRed5ProPlugin version 1.0.0`

## Project Structure

```
example-plugin/
├── src/main/java/com/example/
│   ├── MyRed5ProPlugin.java          # Main plugin class
│   ├── listener/
│   │   └── MyProStreamListener.java  # Stream listener example
│   └── servlet/
│       └── MyServlet.java            # HTTP servlet example
├── src/main/webapp/WEB-INF/
│   └── web.xml                       # Servlet configuration
├── pom.xml                           # Maven build configuration
└── Red5Pro-formatter.xml             # Code formatting rules
```

## Customizing for Your Plugin

### 1. Update Project Identity

Edit `pom.xml` and change:
- `<groupId>` - Your organization/package name
- `<artifactId>` - Your plugin name
- `<name>` - Display name
- `<description>` - What your plugin does

### 2. Rename Plugin Class

1. Rename `MyRed5ProPlugin.java` to your plugin name (e.g., `MyAwesomePlugin.java`)
2. Update the `NAME` constant inside the class
3. Update the manifest entry in `pom.xml`:
   ```xml
   <Red5Pro-Plugin-Main-Class>com.yourpackage.YourPluginName</Red5Pro-Plugin-Main-Class>
   <YourPluginName-Version>${project.version} - ${buildNumber} (on: ${timestamp})</YourPluginName-Version>
   ```

### 3. Update Package Names

Rename packages from `com.example` to your organization's package structure.

### 4. Implement Your Functionality

- **Add stream processing**: Implement or extend `MyProStreamListener`
- **Add HTTP endpoints**: Extend `MyServlet` or create new servlets
- **Add startup logic**: Put initialization code in `doStartProPlugin()`
- **Add cleanup logic**: Put shutdown code in `doStopProPlugin()`

## Key Concepts

### Plugin Lifecycle

1. **Initialization**: Red5 Pro calls `doStartProPlugin()` when loading
2. **Runtime**: Your plugin can access Red5 Pro APIs, register listeners, start background tasks
3. **Shutdown**: Red5 Pro calls `doStopProPlugin()` before unloading

### Stream Listeners

Stream listeners intercept media packets (audio/video/data) as they flow through the server:

```java
ProStream proStream = ProStreamService.getProStream(scope, streamName);
proStream.addStreamListener(myListener);
```

### Background Tasks

Use the plugin's executor service for async work:

```java
MyRed5ProPlugin.submit(() -> {
    // Your background task
});

// Or scheduled tasks
MyRed5ProPlugin.schedule(task, initialDelay, repeatDelay);
```

### Servlet Integration

Servlets provide HTTP endpoints for your plugin:
1. Create servlet class extending `HttpServlet` or `MyServlet`
2. Register in `src/main/webapp/WEB-INF/web.xml`
3. Access plugin via `PluginRegistry.getPlugin()`

## Dependencies

All Red5 Pro and Red5 Server dependencies use `<scope>provided</scope>` because they're supplied by the server at runtime. Only include dependencies in your plugin JAR that are NOT already in the Red5 Pro Server.

Common provided dependencies:
- Red5 Pro core libraries
- Red5 Server
- Spring Framework (6.2.x)
- Apache MINA
- SLF4J/Logback
- Apache Commons libraries
- Tomcat servlet API

## Troubleshooting

### Plugin Not Loading

- Check `log/red5.log` for errors
- Verify the `Red5Pro-Plugin-Main-Class` in the JAR manifest points to your plugin class
- Ensure your plugin class extends `Red5ProPlugin`
- Check for missing dependencies or version conflicts

### Build Failures

- Verify Java version: `java -version` (must be 21+)
- Verify Maven version: `mvn -version` (must be 3.6.0+)
- Check that Red5 Pro JARs are in your local Maven repository: `ls ~/.m2/repository/com/red5pro/`
- Run with debug output: `mvn clean package -X`

### ClassNotFoundException at Runtime

The dependency scope is likely wrong. Red5 Pro-provided libraries should use `<scope>provided</scope>`. Only third-party libraries not in Red5 Pro should be bundled (default scope or `<scope>compile</scope>`).

## Version Compatibility

This example is configured for:
- **Red5 Pro Server**: 15.0.0
- **Red5 Server**: 2.0.22
- **Spring Framework**: 6.2.10
- **Java**: 21+

Adjust versions in `pom.xml` to match your Red5 Pro Server installation.

## Resources

- Red5 Pro Documentation: https://www.red5.net/docs/
- Red5 Pro Support: https://account.red5.net/
- Example Plugin Source: https://github.com/red5pro/example-plugin

## License

Update with your license information.
