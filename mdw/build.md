## building mdw6

1 - Edit gradle.properties to set the new build numbers.
    - mdwVersion.
    - mdwDesignerVersion (latest published version)
    
2 - Edit mdw-workflow/.settings/com.centurylink.mdw.plugin.xml:
    - mdwFramework

3 - Edit mdw-hub/package.json:
    - version (omit -SNAPSHOT)

4 - Edit mdw-hub/bower.json:
    - version (omit -SNAPSHOT)

5 - Edit mdw-hub/manifest.yaml:
    - path (point to new war version)
   
6 - Commit and push these changes to Git.

7 - Perform the Jenkins build (http://lxdenvmtc143.dev.qintra.com:8181/jenkins):
    - MDW6-Build
    - Review console output for errors.
    