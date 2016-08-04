This repository cntains experiments to make Eclipse IDE able to consume the [https://github.com/Microsoft/language-server-protocol],(Language Server protocol).

This has been initiated during the EclipseCon France 2016 Unconference.

for details, see [Documentation Index](/adoc/index.adoc)

At the moment, it provides regular JFace/Platform Text classes for:
* WIP detection of language server for given file (see also issues #3 and #4)
* synchronization of files with Language Server
* diagnostics as problem markers
* completion
* hover
* jump to declaration
* Find References

Extensions to the [https://bugs.eclipse.org/bugs/show_bug.cgi?id=497871](Generic Editor proposal for Eclipse Platform Text) are provided so having the generic editor + this bundle enables the LSP based behavior in the Generic editor. But those classes can be reused in any editor or other extensions.

Contributions are highly welcome using GitHub issues and PR at the moment.

This piece of work is meant to move to some Eclipse.org project then inside the Eclipse IDE package directlyas soon as it is considered stable and isable enough.
