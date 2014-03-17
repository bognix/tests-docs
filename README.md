tests-docs
============

##Compilation
```bash
javac -classpath /usr/lib/jvm/java-1.7.0-openjdk-amd64/lib/tools.jar src/ClassesAndMethodsToXML.java
```

##Usage

```bash
javadoc -sourcepath src/test/java -subpackages com.wikia.webdriver.TestCases -doclet ClassesAndMethodsToXML -docletpath %path to compiled doclet%
```
