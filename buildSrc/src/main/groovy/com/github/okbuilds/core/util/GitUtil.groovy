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
        "git clone ${repo} ${repoDir.absolutePath}".execute()
    }

    /**
     * Fetches all the changes from various remotes to a repo
     * @param repoDir The git repository directory
     */
    static void fetchAll(File repoDir) {
        "git -C ${repoDir.absolutePath} fetch --all".execute()
    }

    /**
     * Checks out a given sha/branch/tag on a repo
     * @param repoDir The git repository directory
     * @param repoDir The git sha/branch/tag
     */
    static void checkout(File repoDir, String sha) {
        "git -C ${repoDir.absolutePath} fetch --all".execute()
    }

    /**
     * Add a new remote url if not already present.
     * @param repoDir The git repository directory
     * @param gitUrl The remote git url
     */
    static void addRemoteIfNeeded(File repoDir, String gitUrl) {
        Set<String> remotes = []
        String existing = "git -C ${repoDir.absolutePath} remote -v".execute().text
        if (existing) {
            remotes.addAll(existing.split('\n')
                    .findAll { !it.empty }
                    .collect { it.split(' ')[1] })
        }
        if (!remotes.contains(gitUrl)) {
            String remote_name = DigestUtils.md5Hex(gitUrl)
            "git -C ${repoDir.absolutePath} remote add ${remote_name} ${gitUrl}"
        }
    }

    /**
     * Cleans and resets the state of a git repository
     * @param repoDir The git repository directory
     */
    static void cleanReset(File repoDir) {
        "git -C ${repoDir.absolutePath} reset --hard".execute()
        "git -C ${repoDir.absolutePath} clean -fdx".execute()
    }
}
