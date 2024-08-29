package tasks

import contributors.*
import retrofit2.Response
import java.util.*

//  the whole loading logic is moved to the background thread, but that still isn't the best use of resources. All of the loading requests go sequentially and the thread is blocked while waiting for the loading result, while it could have been occupied by other tasks.
fun loadContributorsBlocking(service: GitHubService, req: RequestData): List<User> {
    val repos = service
        .getOrgReposCall(req.org)
        .execute() // Executes request and blocks the current thread
        .also {
            println("[loadContributorsBlocking][repos] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
            logRepos(req, it)
        }
        .body() ?: emptyList()

//    return repos.flatMap { repo ->
//        service
//            .getRepoContributorsCall(req.org, repo.name)
//            .execute() // Executes request and blocks the current thread
//            .also {
//                println("[loadContributorsBlocking][contributors] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
//                logUsers(repo, it) }
//            .bodyList()
//    }.aggregate()

    // TODO: get the fist repo's contributes
    return service
        .getRepoContributorsCall(req.org, repos[0].name)
        .execute() // Executes request and blocks the current thread
        .also {
            println("[loadContributorsBlocking][contributors] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
            logUsers(repos[0], it)
        }
        .body() ?: emptyList()
}