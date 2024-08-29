package tasks

import contributors.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

// The loading for each repository can then be started before the result for the previous repository is received (and the corresponding callback is called):
//
//Using callback API
//The Retrofit callback API can help achieve this. The Call.enqueue() function starts an HTTP request and takes a callback as an argument. In this callback, you need to specify what needs to be done after each request.
fun loadContributorsCallbacks(service: GitHubService, req: RequestData, updateResults: (List<User>) -> Unit) {
//    originSolution(service, req, updateResults)
//    firstAttemptedSolution(service, req, updateResults)
//    secondAttemptedSolution(service, req, updateResults)
    thirdAttemptedSolution(service, req, updateResults)
}

private fun originSolution(service: GitHubService, req: RequestData, updateResults: (List<User>) -> Unit) {
    println("[loadContributorsCallbacks 1] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
    service.getOrgReposCall(req.org).onResponse { responseRepos ->
        println("[loadContributorsCallbacks 2] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
        logRepos(req, responseRepos)
        val repos = responseRepos.bodyList()
        val allUsers = mutableListOf<User>()

        for (repo in repos) {
            service.getRepoContributorsCall(req.org, repo.name).onResponse { responseUsers ->
                logUsers(repo, responseUsers)
                println("[loadContributorsCallbacks 3] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
                val users = responseUsers.bodyList()
                allUsers += users
            }
        }
        // TODO: Why this code doesn't work? How to fix that?
        updateResults(allUsers)
    }
}

/**
 * withIndex
 */
private fun firstAttemptedSolution(service: GitHubService, req: RequestData, updateResults: (List<User>) -> Unit) {
    service.getOrgReposCall(req.org).onResponse { responseRepos ->
        println("[loadContributorsCallbacks 2] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
        logRepos(req, responseRepos)
        val repos = responseRepos.bodyList()
        val allUsers = mutableListOf<User>()

        for ((index, repo) in repos.withIndex()) {
            service.getRepoContributorsCall(req.org, repo.name).onResponse { responseUsers ->
                logUsers(repo, responseUsers)
                println("[loadContributorsCallbacks 3] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
                val users = responseUsers.bodyList()
                allUsers += users
                if (index == repos.lastIndex) {
                    updateResults(allUsers.aggregate())
                }
            }
        }
    }
}

/**
 * AtomicInteger
 */
private fun secondAttemptedSolution(service: GitHubService, req: RequestData, updateResults: (List<User>) -> Unit) {
    service.getOrgReposCall(req.org).onResponse { responseRepos ->
        println("[loadContributorsCallbacks 2] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
        logRepos(req, responseRepos)
        val repos = responseRepos.bodyList()
        val allUsers = mutableListOf<User>()

        val numberOfProcessed = AtomicInteger()
        for (repo in repos) {
            service.getRepoContributorsCall(req.org, repo.name).onResponse { responseUsers ->
                logUsers(repo, responseUsers)
                println("[loadContributorsCallbacks 3] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
                val users = responseUsers.bodyList()
                allUsers += users
                if (numberOfProcessed.incrementAndGet() == repos.size) {
                    updateResults(allUsers.aggregate())
                }
            }
        }
    }
}

/**
 * CountDownLatch
 */
private fun thirdAttemptedSolution(service: GitHubService, req: RequestData, updateResults: (List<User>) -> Unit) {
    service.getOrgReposCall(req.org).onResponse { responseRepos ->
        println("[loadContributorsCallbacks 2] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
        logRepos(req, responseRepos)
        val repos = responseRepos.bodyList()
        val allUsers = mutableListOf<User>()

        val countDownLatch = CountDownLatch(repos.size)
        for (repo in repos) {
            service.getRepoContributorsCall(req.org, repo.name).onResponse { responseUsers ->
                logUsers(repo, responseUsers)
                println("[loadContributorsCallbacks 3] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
                val users = responseUsers.bodyList()
                allUsers += users
                countDownLatch.countDown()
            }
        }
        // wait until the latch is counted down to zero before updating the results.
        countDownLatch.await()
        updateResults(allUsers.aggregate())
    }
}

inline fun <T> Call<T>.onResponse(crossinline callback: (Response<T>) -> Unit) {
    enqueue(object : Callback<T> { // Call
        override fun onResponse(call: Call<T>, response: Response<T>) {
            callback(response)
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            log.error("Call failed", t)
        }
    })
}
