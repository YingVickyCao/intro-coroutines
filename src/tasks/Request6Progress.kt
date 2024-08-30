package tasks

import contributors.*
import java.util.*

suspend fun loadContributorsProgress(
    service: GitHubService, req: RequestData, updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    println("[PROGRESS 3] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
    //  loading data
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }

    // calling updateResults on intermediate states
    var allUsers = emptyList<User>()
    for ((index, repo) in repos.withIndex()) {
        val users = service.getRepoContributors(req.org, repo.name).also { logUsers(repo, it) }
        allUsers = (allUsers + users).aggregate()
        println("[PROGRESS 4] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
        updateResults(allUsers, index == repos.lastIndex)
    }
    println("[PROGRESS 5] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
}
