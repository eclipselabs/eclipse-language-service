Contributors Guide
================

1. Run `git submodule update --init --recursive` to fetch GenericEditor submodule.
1. Run `mvn package -P build-individual-bundles`. This will prepare project dependencies so that you can use in target platform in Eclipse development environment.
1. Open `releng/platform.target` in PDE, and set it as your target plaform.

#### Building the project:

1. Run `mvn verify -P build-individual-bundles`.

