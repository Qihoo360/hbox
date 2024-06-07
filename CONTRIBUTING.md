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
Bump project major version: `./mvnw validate -Pbump-major-version`
Bump project minor version: `./mvnw validate -Pbump-minor-version`
Sort pom.xml: `./mvnw sortpom:sort`

Release Steps
===

```bash
# 1. pass integration tests
./mvnw clean verify

# 2. pass smoking test cases in the tests/ folder
# run on gateways

# 3. prepare release version, git tag and next version
./mvnw release:clean release:prepare -DpushChanges=false

# 4. push to git repo
git push --follow-tags

# 5. cleanup and sync with the remote repo
./mvnw release:clean
git fetch
```
