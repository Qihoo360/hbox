Download Releases
===

http://maven.corp.mediav.com/nexus/content/repositories/releases/net/qihoo/hbox-dist/

CI job
===

http://cd2ss.jx.shbt.qihoo.net:8080/job/hbox/

Dev Tasks
===

Bump maven plugins: `./mvnw versions:display-plugin-updates -pl .`
Bump maven wrapper: `./mvnw wrapper:wrapper`
Sort pom.xml: `./mvnw sortpom:sort`

Release Steps
===

```bash
# 1. pass integration tests
./mvnw clean verify

# 2. pass smoking test cases in the tests/ folder
# run on gateways

# 3. prepare release version, git tag and next version
./mvnw release:clean release:prepare

# 4. cleanup
./mvnw release:clean

# 5. sync with the remote repo
git fetch

# 6. trigger ci jobs for the new tag
# jenkins does not auto build jobs for tag (JENKINS-47496)
# and we now cannot install jenkins-plugin https://plugins.jenkins.io/basic-branch-build-strategies/
#
# url: http://cd2ss.jx.shbt.qihoo.net:8080/job/hbox/
```
