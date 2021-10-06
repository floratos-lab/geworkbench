# geWorkbench Developer Guide

This document provides a guide for developers of geWorkbench.

## Project Structure

This section describes the directory structure of the project. The root directory structure contains the project and library files. It also contains this file, the `java.policy` file, and the `build.xml`
ant build file.

* `components` - The top-level directory for geWorkbench components.
* `data` - Sample data files for the project.
* `lib` - Library files for core geWorkbench.
* `src` - Source code for core geWorkbench.

Each component is in a subdirectory of the `components` directory.
Its directory structure is as follows:

* `lib` - Library files depended on by this component. Optional.
* `src` - Source code for the component.
* `test` - Unit test for the component.
  
Note that the components can depend on the core classes (those defined in `./src`) and the core libraries
(those defined in `./lib`). However, components cannot depend on each other, and the core classes cannot
depend on components.

## Creating a New Component

A new component should be added as a new subdirectory of `components`. The subdirectory should be given a simple, descriptive,
lower-case name. It should contain `src` and `lib` directories. Most importantly, the component must not depend
on the classes or libraries of any other component, nor should another component depend on it. Also, the core geWorkbench classes
should not depend on its classes or libraries. The `build.xml` will enforce this.

## Coding Style

[The coding standards outlined by Oracle](https://www.oracle.com/java/technologies/javase/codeconventions-contents.html)
are observed.
Here are the coding standards that are particularly important to us:

1. **Capitalization** - Constants must be in all-capitals, with underscores between words:
```java
  public static final int MAXIMUM_FILES = 100;
```
**No** other identifiers may contain underscores but constants.
Class, interface, enum and annotation names must be capitalized, with all subsequent words also capitalized:
```java
  public class SampleClass extends AnotherSampleClass {
```
Variable and member names must have their first letter lower-case, and subsequent words capitalized:
```java
  private String fieldName;

  private abstract void processFile(File file);
```
Some developers differentiate between method variables and class members by prepending `m_` or just `_` to member names.
This is not a standard practice, so is not observed.
Identifiers in property files are all lower-case with periods separating words:
```properties
  maximum.files=100
```
Type wildcards in generics must be a single capital letter:
```java
  public interface Generic<T, S extends Serializable> {
```
2. **Annotations** - Annotations can either appear on the same line as their associated declaration, or on the line before it:
```java
  @Subscribe public void receive(Integer value) {

  @Script(dependencies = {STATE_UNINITIALIZED, STATE_COMPLETE}, result = STATE_INITIALIZED)
  public void initialize() {
```
3. **Variable Naming** - Variables should be given verbose names, even if they are only used in relatively small code blocks. Using the auto-complete functionality of modern IDEs makes compliance with this rule painless. Exceptions are simple index variables, such as those introduced in `for(;;)` statements:
```java
  for (int i = 0; i < 100; i++) {
```
4. **Braces** - All `if`, `while` and similar block structures must be surrounded by braces, even if the body of the block consists of only one statement:
```java
  if (index < 100) {
      System.out.println("index= " + index);
  }
```
5. **No Shortcuts** - Java provides some C-style shortcuts, such as the `++`, `+=` and `?:` operators. The `++`, `+=` operators can be used by themselves (for example, `++` is often used in a `for` statement) but they should not be included in other statements. The `?:` operator should only be used very sparingly, usually in statements that construct strings. Here is an example of a **bad** use of `++`:
```java
  while (i < 100) {
    System.out.println("i= " + i++);
  }
```
This is preferred:
```java
  while (i < 100) {
    System.out.println("i= " + i);
    i++;
  }
```
This is an acceptable use of `?:`:
```java
  boolean available;
  ...
  System.out.println("The file is " + (available ? "available" : "unavailable") + ".");
```
However, it is a safe bet to never use `?:`.

6. **Tabs** - A tab-width of 4 should be used.

## Code Documentation

Code must be documented using the [Javadoc](https://www.oracle.com/java/technologies/javase/javadoc-tool-doc.html) standard.
All classes, interfaces, enums and annotations must have an introductory Javadoc explaining its purpose.
All public methods must also have explanatory Javadocs.
Methods that perform non-trivial operations should have normal comments (not Javadoc) that explain the code.
Such comments should precede the line (or lines) of code that they describe.
For example:
```java
  /**
   * Shuts down the componentRegistry (and terminates any pending aysnchronous dispatches).
   */
  public void shutdown() {
      // Iterate through all active synch models
      Collection<SynchModel> models = synchModels.values();
      for (SynchModel synchModel : models) {
          // Shut down the synch model
          synchModel.shutdown();
      }
  }
```

## Packages

Package names should follow the inverse-domain rule. If the domain of your organization is
 `subdomain.domain.tld`, then the package structure should be rooted at `tld.domain.subdomain`.
Capital letters or underscore characters should never appear in package names.

Otherwise, the organization of packages is left up to the developer.
