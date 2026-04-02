Releasing
=========

## Prepare for Release

1. Change the version in `buildSrc/gradle.properties` to a non-SNAPSHOT version. (i.e., remove the -SNAPSHOT suffix)
2. Update the `README.md` and README-zh.md with the new version.
3. Update the `CHANGELOG.md` for the impending release.
4. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
5. `git tag -a X.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
6. `git push && git push --tags`


## Push New Release
1. Using github UI to create a release from a tag (https://github.com/uber/okbuck/releases/new?tag=vX.Y.Z)
	1. Click on Tags
	2. Find your new tag and select "Create Release" from the context menu.
	3. Auto-generate and edit release notes as necessary.
2. Publish to Maven Central via Sonatype Central Portal:
	```bash
	./gradlew clean :plugin:publishToSonatype closeSonatypeStagingRepository --no-daemon --no-parallel
	```
	This will upload the artifacts to a staging repository and close it for review.

3. Review and release the deployment:
	- Go to https://central.sonatype.com/publishing
	- Review the staged deployment
	- Click "Publish" to release to Maven Central
	
	**Alternative:** To publish automatically without manual review:
	```bash
	./gradlew clean :plugin:publishToSonatype closeAndReleaseSonatypeStagingRepository --no-daemon --no-parallel
	```

**Note:** Publishing requires Central Portal User Tokens in `~/.gradle/gradle.properties`:
```properties
mavenCentralUsername=<your-user-token-username>
mavenCentralPassword=<your-user-token-password>
```
Generate User Tokens at: https://central.sonatype.com/account


## Prepare for Next Release

1. Update the `buildSrc/gradle.properties` to the next SNAPSHOT version.
2. `git commit -am "Prepare next development version."`
3. `git push`