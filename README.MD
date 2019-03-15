# brXM Unit Tester

Unit testing framework for brXM's delivery tier

## Installation

1. Add dependency to pom.xml
```xml
<dependencyManagement>
    <dependency>
        <groupId>com.bloomreach.ps</groupId>
        <artifactId>brxm-unit-tester</artifactId>
        <version>${brxm-unit-tester.version}</version>
        <scope>test</scope>
    </dependency>
    ...
</dependencyManagement>
```

2. Add dependency to site pom.xml.
```xml
<dependency>
    <groupId>com.bloomreach.ps</groupId>
    <artifactId>brxm-unit-tester</artifactId>
</dependency>
```

## Usage
Within the package there are classes to extend from:

* BaseHippoTest
* SimpleHippoTest

### Importing nodes
Use com.bloomreach.ps.brxm.jcr.repository.utils.YamlImporter for importing YAML files.[Example](https://code.onehippo.org/sandbox/brxm-unit-tester/blob/master/demo/myhippoproject/site/src/test/java/org/example/EssentialsListComponentTest.java)

#### YAML

If you are importing yaml that references images, make sure you choose the zip export option. Unzip the export
in the classpath.

```java
java.net.URL resource = getClass().getResource("/news.yaml");
YamlImporter.importYaml(resource, rootNode, "/content/documents/mychannel", "hippostd:folder");

```

One of the best ways to learn about how to use this library is by looking at an [example](https://code.onehippo.org/sandbox/brxm-unit-tester/blob/master/src/test/java/com/bloomreach/brxm/unittester/demo/EssentialsListComponentTest.java)