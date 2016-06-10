# Eclipse integration for language service (experiment)


This repository contains experiments to make Eclipse IDE able to consume the Microsoft Language Server protocol.

This has been initiated during the EclipseCon France 2016 Unconference.

for details, see [Documentation Index](/adoc/index.adoc)

At the moment, it only provides the extension to add completion to the JavaScript editor. Some related future work will be:

1. Adding extension points on the generic Text Editor to hook strategies for completion, coloration, Open Declaration... This work has to be done in Eclipse Platform UI directly.
2. Work on a Language Client for Eclipse IDE. This work can remain implemented as part of the repository at the moment; but it should strongly be considered for integration in some Eclipse.org project ASAP.
3. Create extensions to 1. using the Language Client created in 2. This work can remain implemented as part of the repository at the moment; but it should strongly be considered for integration in some Eclipse.org project ASAP.

Step 1. and 2. can be worked on simultaneously. Integration (step 3) can happen progressively and integrate change for 1. and 2. as they arrive.
