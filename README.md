This repository contains experiments to make Eclipse IDE able to consume the [Language Server protocol](https://github.com/Microsoft/language-server-protocol).
This has been initiated during the EclipseCon France 2016 Unconference.

[![ScreenShot](http://content.screencast.com/users/mistria/folders/Default/media/1a860eda-8a50-4668-874c-ee2dd2ef213c/FirstFrame.jpg)](http://www.screencast.com/t/Xs3TtaQM)


for details, see [Documentation Index](/adoc/index.adoc)

At the moment, it provides regular JFace/Platform Text classes for:
* WIP detection of language server for given file (see also issues #3 and #4)
* synchronization of files with Language Server
* diagnostics as problem markers
* completion
* hover
* jump to declaration
* Find References

Extensions to the [Generic Editor proposal for Eclipse Platform Text](https://bugs.eclipse.org/bugs/show_bug.cgi?id=497871) are provided so having the generic editor + this bundle enables the LSP based behavior in the Generic editor. But those classes can be reused in any editor or other extensions. Examples of integration contain:
* C# edition in Eclipse IDE using OmniSharp LSP implementation
* JSON (with schema) using VSCode LSP impl
* CSS using VSCode LSP impl.

Contributions are highly welcome using GitHub issues and PR at the moment.

This piece of work is meant to move to some Eclipse.org project then inside the Eclipse IDE package directly as soon as it is considered stable and isable enough.
