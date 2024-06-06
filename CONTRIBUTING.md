Download Releases
===

https://github.com/Qihoo360/hbox/releases

CI job
===

https://github.com/Qihoo360/hbox/actions

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
```
