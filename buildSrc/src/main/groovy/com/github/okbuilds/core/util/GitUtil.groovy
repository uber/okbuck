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
        CmdUtil.run("git clone ${repo} ${repoDir.absolutePath}")
    }

    /**
     * Fetches all the changes from various remotes to a repo
     * @param repoDir The git repository directory
     */
    static void fetchAll(File repoDir) {
        CmdUtil.run("git -C ${repoDir.absolutePath} fetch --all")
    }

    /**
     * Checks out a given ref (sha/branch/tag) on a repo
     * @param repoDir The git repository directory
     * @param repoDir The git ref/branch/tag
     */
    static void checkout(File repoDir, String ref) {
        CmdUtil.run("git -C ${repoDir.absolutePath} checkout ${ref}")
    }

    /**
     * Add a remote git url.
     * @param repoDir The git repository directory
     * @param gitUrl The remote git url
     */
    static void addRemote(File repoDir, String gitUrl) {
        CmdUtil.run("git -C ${repoDir.absolutePath} remote add ${remoteName(gitUrl)} ${gitUrl}", true)
    }

    /**
     * Get the remote name for a git url
     * @param gitUrl The git repository url
     * @return The remote name
     */
    static String remoteName(String gitUrl) {
        return DigestUtils.md5Hex(gitUrl)
    }
}
