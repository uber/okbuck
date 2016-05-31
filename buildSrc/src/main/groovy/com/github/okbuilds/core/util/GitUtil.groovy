package com.github.okbuilds.core.util

import org.apache.commons.codec.digest.DigestUtils
/**
 * Utility class for operations on a git repository.
 */
class GitUtil {

    private GitUtil() {}

    /**
     * Perform a clone of a repository
     * @param repo The git repository
     * @param repoDir The directory to clone to
     */
    static void clone(String repo, File repoDir = "") {
        "git clone ${repo} ${repoDir.absolutePath}".execute().waitFor()
    }

    /**
     * Fetches all the changes from various remotes to a repo
     * @param repoDir The git repository directory
     */
    static void fetchAll(File repoDir) {
        "git -C ${repoDir.absolutePath} fetch --all".execute().waitFor()
    }

    /**
     * Checks out a given sha/branch/tag on a repo
     * @param repoDir The git repository directory
     * @param repoDir The git sha/branch/tag
     */
    static void checkout(File repoDir, String sha, String remoteName) {
        "git -C ${repoDir.absolutePath} checkout ${remoteName}/${sha}".execute().waitFor()
    }

    /**
     * Add a remote git url.
     * @param repoDir The git repository directory
     * @param gitUrl The remote git url
     */
    static void addRemote(File repoDir, String gitUrl) {
        "git -C ${repoDir.absolutePath} remote add ${remoteName(gitUrl)} ${gitUrl}".execute().waitFor()
    }

    /**
     * Get the remote name for a git url
     * @param gitUrl The git repository url
     * @return The remote name
     */
    static String remoteName(String gitUrl) {
        return DigestUtils.md5Hex(gitUrl)
    }

    /**
     * Cleans and resets the state of a git repository
     * @param repoDir The git repository directory
     */
    static void cleanReset(File repoDir) {
        "git -C ${repoDir.absolutePath} reset --hard".execute().waitFor()
        "git -C ${repoDir.absolutePath} clean -fdx".execute().waitFor()
    }
}
