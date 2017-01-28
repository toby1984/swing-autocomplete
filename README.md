# swing-autocomplete

Adds auto-complete behaviour to any Java Swing JTextComponent (JEditorPane,JTextField,etc.).

A demo application can be found inside src/test

# Controls / Input behaviour

- Pressing CONTROL-SPACE triggers auto-complete proposals (the default implementation will pick the word under/right before the caret position when fetching the initial proposals)
- Pressing either the ENTER or the SPACE key will insert the currently selected proposal into the text component and dispose of the popup
- CURSOR-UP / CURSOR-DOWN navigate through the available proposals
- The proposal popup will be disposed automatically when the text component loses focus
- Auto-completion will not start when the initial trigger didn't find at least one proposal

![screenshot](https://github.com/toby1984/swing-autocomplete/blob/master/screenshot.png?raw=true)

# How to use

- Clone this repository
- Run 'mvn install' inside the top-level folder (requires Maven 3.x and JDK >=1.7)
- Add the following snippet to your Maven pom.xml
```
<dependency>
  <groupId>de.codesourcery.swing.autocomplete</groupId>
  <artifactId>swing-autocomplete</artifactId>
  <version>1.0.4</version>
</dependency>
```


