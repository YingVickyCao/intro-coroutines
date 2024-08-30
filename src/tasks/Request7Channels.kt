package tasks

import contributors.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.*

suspend fun loadContributorsChannels(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    println("[CHANNELS 3] thread(" + Thread.currentThread().name + "," + Thread.currentThread().id + ")," + Date().toString())
    coroutineScope {
        println("[CHANNELS 4] thread(" + Thread.currentThread().name + "," + Thread.currentThread().id + ")," + Date().toString())
        //  loading data
        val repos = service
            .getOrgRepos(req.org)
            .subList(0, 5) // TODO: [0.5]
            .also { logRepos(req, it) }

        val channel = Channel<List<User>>()
        for (repo in repos) {
            launch {
                println("[CHANNELS 5] thread(" + Thread.currentThread().name + "," + Thread.currentThread().id + ")," + Date().toString())
                val users = service.getRepoContributors(req.org, repo.name)
                    .also { logUsers(repo, it) }
                channel.send(users)
            }
        }
        var allUsers = emptyList<User>()
        repeat(repos.size) {
            println("[CHANNELS 6] thread(" + Thread.currentThread().name + "," + Thread.currentThread().id + ")," + Date().toString())
            val users = channel.receive()
            allUsers = (allUsers + users).aggregate()
            updateResults(allUsers, it == repos.lastIndex)
        }
        println("[CHANNELS 7] thread(" + Thread.currentThread().name + "," + Thread.currentThread().id + ")," + Date().toString())
    }
    println("[CHANNELS 8] thread(" + Thread.currentThread().name + "," + Thread.currentThread().id + ")," + Date().toString())
}
