package tasks

import contributors.*
import kotlinx.coroutines.*
import java.util.*

suspend fun loadContributorsConcurrent(service: GitHubService, req: RequestData): List<User> =
    coroutineScope {
        println("[CONCURRENT 3] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
        val repos = service.getOrgRepos(req.org)
            .subList(0, 5) // TODO:0=5
            .also { logRepos(req, it) }

        val deferreds: List<Deferred<List<User>>> = repos.map { repo ->
            println("[CONCURRENT 4] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())

// CoroutineDispatcher determines what thread or threads the corresponding coroutine should be run on. If you don't specify one as an argument, async will use the dispatcher from the outer scope.
            async { // 5/6 underlying thread name:AWT-EventQueue-0
                // Dispatchers.Default represents a shared pool of threads on the JVM. This pool provides a means for parallel execution. It consists of as many threads as there are CPU cores available, but it will still have two threads if there's only one core.
//            async(Dispatchers.Default) { // 5/6: underlying thread name:DefaultDispatcher-worker-2, ....,DefaultDispatcher-worker-N
                println("[CONCURRENT 5] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
                delay(5000L)
                service.getRepoContributors(req.org, repo.name).also {
                    println("[CONCURRENT 6] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
                    logUsers(repo, it)
                }
            }
        }
        println("[CONCURRENT 7] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
        deferreds.awaitAll().flatten().aggregate()
    }