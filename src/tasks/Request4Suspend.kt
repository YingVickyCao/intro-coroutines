package tasks

import contributors.*
import retrofit2.Response
import java.util.*

suspend fun loadContributorsSuspend(service: GitHubService, req: RequestData): List<User> {
    println("[SUSPEND 3] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
    val repos = service
        .getOrgRepos(req.org)
        .also {
            println("[SUSPEND 4] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
            logRepos(req, it)
        }

    return repos.flatMap { repo ->
        service
            .getRepoContributors(req.org, repo.name)
            .also {
                println("[SUSPEND 5] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
                println("[loadContributorsBlocking][contributors] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
                logUsers(repo, it)
            }
    }.aggregate()
}

fun <T> Response<List<T>>.bodyList(): List<T> {
    return body() ?: emptyList()
}