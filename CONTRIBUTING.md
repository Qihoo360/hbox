Download Releases
===

https://github.com/Qihoo360/hbox/releases

CI job
===

https://github.com/Qihoo360/hbox/actions

Dev Tasks
===

* Bump maven plugins: `./mvnw versions:display-plugin-updates -pl .`
* Bump maven wrapper: `./mvnw wrapper:wrapper`
* Bump project major version: `./mvnw validate -Pbump-major-version`
* Bump project minor version: `./mvnw validate -Pbump-minor-version`
* Format codes: `./mvnw spotless:apply`

Release Steps
===

0. NOTE: DO _NOT_ create tag on the web page, instead,
RUN the following scripts manually on a dev machine by release managers.

0. Checkout or clone the latest `master` branch

0. Pass integration tests

   ```bash
   ./mvnw clean verify
   ```

0. Pass smoking test cases in the `tests/` folder on some gateway machines

0. Prepare release version, git tag and next version
   ```bash
   ./mvnw release:clean release:prepare -DpushChanges=false
   ```

0. Push to git repo
   ```bash
   git push --follow-tags
   ```

0. Cleanup and sync with the remote repo
   ```bash
   ./mvnw release:clean
   git fetch
   ```

0. Make sure github action jobs are success: https://github.com/Qihoo360/hbox/actions/workflows/verify-and-release.yml

0. Edit the draft release at https://github.com/Qihoo360/hbox/releases ,
   update the changelog and release notes, then publish the new release.
