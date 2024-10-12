# Pitest-Gradle

<!-- Plugin description -->
This is a plugin for IntelliJ IDEA that allows you to run PIT mutation testing on your Gradle project.
<!-- Plugin description end -->

## Features
- Run Pitest on your Java Gradle projects
- View mutation test results in a tool window and navigate to the source code
- Decorate the editor of the source code with mutation test results

## Installation

1. Download the latest release from the [releases page]()
2. Open IntelliJ IDEA
3. Go to `File` -> `Settings` -> `Plugins` -> `Install Plugin from Disk...`
4. Select the downloaded `.zip` file

## Usage

1. Open your junit test file, run the test make sure it is passing
2. Right click on the test file and select `Run PIT Mutation Test`
3. Input the target mutation test class, for example `com.example.MyClass` or `MyClass`, and click `OK`
4. The mutation test will run and the results will be displayed in the `Mutation Tool Window`

## Known Issues

- [ ] [!due to issue](https://stackoverflow.com/questions/70448459/gradle-error-in-ijresolvers-gradle-when-running-using-intellij-idea/70597547#70597547) you need to disable kotlin plugin if using old version of gradle like 4.4 
- [ ] it cannot find the classpath if the project is not managed by Gradle, click on the Project structure and import the project as Gradle project
- [ ] it cannot find the resource directory if you don't mark the directory as resource root, right click on the directory and mark as resource root
