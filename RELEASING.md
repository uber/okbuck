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

### 1. Create GitHub Release
Using github UI to create a release from a tag (https://github.com/uber/okbuck/releases/new?tag=vX.Y.Z)
1. Click on Tags
2. Find your new tag and select "Create Release" from the context menu.
3. Auto-generate and edit release notes as necessary.

### 2. Publish to Maven Central

The plugin uses a custom publishing task that uploads to Maven Central Portal.

**Prerequisites:**
- Maven Central Portal credentials (username and token)
- Get credentials from: https://central.sonatype.com/account
- Ensure PGP signing is configured (see buildSrc/gradle.properties)

**Publish command:**
```bash
./gradlew publishToCentralPortal \
  -PmavenCentralUsername=<your-username> \
  -PmavenCentralPassword=<your-token>
```

Alternatively, set environment variables to avoid passing credentials on command line:
```bash
export MAVEN_CENTRAL_USERNAME=<your-username>
export MAVEN_CENTRAL_PASSWORD=<your-token>
./gradlew publishToCentralPortal
```

**What happens:**
- Builds all artifacts (JAR, sources, javadoc, POM, module metadata)
- Signs all artifacts with PGP
- Generates MD5 and SHA1 checksums
- Creates a deployment bundle (ZIP file)
- Uploads to Maven Central Portal

**After upload:**
- Log into https://central.sonatype.com/publishing
- Review and publish the deployment (Manual step required)
- Publication typically takes 15-30 minutes to sync to Maven Central

**Verify locally before uploading:**
```bash
./gradlew publishToCentralPortal -PskipUpload=true
```
This creates the bundle at `buildSrc/build/okbuck-X.Y.Z.zip` without uploading.


## Prepare for Next Release

1. Update the `buildSrc/gradle.properties` to the next SNAPSHOT version.
2. `git commit -am "Prepare next development version."`
3. `git push`