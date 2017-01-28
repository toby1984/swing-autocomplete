# swing-autocomplete

Adds auto-complete behaviour to any Java Swing JTextComponent (JEditorPane,JTextField,etc.). You need JDK 1.8 because I'm using lambda expressions.

# How to use

- Clone this repository
- Run 'mvn install' inside the top-level folder (requires Maven 3.x and JDK >=1.8)
- Add the following snippet to your Maven pom.xml
```
<dependency>
  <groupId>de.codesourcery.swing.autocomplete</groupId>
  <artifactId>swing-autocomplete</artifactId>
  <version>1.0.0</version>
</dependency>
```

A demo application can be found inside src/test
