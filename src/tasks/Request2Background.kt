package tasks

import contributors.GitHubService
import contributors.RequestData
import contributors.User
import java.util.*
import kotlin.concurrent.thread

fun loadContributorsBackground(service: GitHubService, req: RequestData, updateResults: (List<User>) -> Unit) {
    println("[BACKGROUND 4] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
    thread { // Create a new thread, and runs codes of this block in this new thread.
        println("[BACKGROUND 5] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
        loadContributorsBlocking(service, req)
        println("[BACKGROUND 6] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
    }
}