package contributors

import contributors.Contributors.LoadingStatus.*
import contributors.Variant.*
import kotlinx.coroutines.*
import tasks.*
import java.awt.event.ActionListener
import java.util.Date
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

enum class Variant {
    BLOCKING,         // Request1Blocking
    BACKGROUND,       // Request2Background
    CALLBACKS,        // Request3Callbacks
    SUSPEND,          // Request4Coroutine
    CONCURRENT,       // Request5Concurrent
    NOT_CANCELLABLE,  // Request6NotCancellable
    PROGRESS,         // Request6Progress
    CHANNELS          // Request7Channels
}

interface Contributors : CoroutineScope {

    val job: Job

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    fun init() {
        // Start a new loading on 'load' click
        addLoadListener {
            saveParams()
            loadContributors()
        }

        // Save preferences and exit on closing the window
        addOnWindowClosingListener {
            job.cancel()
            saveParams()
            exitProcess(0)
        }

        // Load stored params (user & password values)
        loadInitialParams()
    }

    fun loadContributors() {
        val (username, password, org, _) = getParams()
        val req = RequestData(username, password, org)

        clearResults()
        val service = createGitHubService(req.username, req.password)

        val startTime = System.currentTimeMillis()
        when (getSelectedVariant()) {
            BLOCKING -> { // Blocking UI thread
                println("[BLOCKING 1] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
                val users = loadContributorsBlocking(service, req)
                updateResults(users, startTime)
                println("[BLOCKING 2] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
            }

            BACKGROUND -> { // Blocking a background thread
                println("[BACKGROUND 1] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
                loadContributorsBackground(service, req) { users ->
                    SwingUtilities.invokeLater {
                        println("[BACKGROUND 2] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
                        updateResults(users, startTime)
                    }
                }
                println("[BACKGROUND 3] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
            }

            CALLBACKS -> { // Using callbacks
                println("[CALLBACKS 1] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
                loadContributorsCallbacks(service, req) { users ->
                    SwingUtilities.invokeLater {
                        updateResults(users, startTime)
                    }
                }
                println("[CALLBACKS 2] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
            }

            SUSPEND -> { // Using coroutines
                println("[SUSPEND 1] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
                launch {  //launch a new suspendable computation (called a coroutine, lauch starts a new coroutine responsible for loading data and showing the results.) :when performing network
                    // requests, it is suspended and releases the underlying thread. when network request returns the result, the computation is resumed.
                    println("[SUSPEND 2] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
                    val users = loadContributorsSuspend(service, req)
                    updateResults(users, startTime)
                    println("[SUSPEND 6] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
                }.setUpCancellation()
                println("[SUSPEND 7] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
            }

            CONCURRENT -> { // Performing requests concurrently
                println("[CONCURRENT 1] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
                val start = System.currentTimeMillis()
                launch(Dispatchers.Default) {
                    println("[CONCURRENT 2] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
//                    val users = loadContributorsConcurrent(service, req)
//                    val users = loadContributorsNotCallable(service, req)
                    val users = loadContributorsNotCancellable(service, req)
                    println("[CONCURRENT 8] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
//                    updateResults(users, startTime)
//                    launch(Dispatchers.Main) {
//                        println("[CONCURRENT 9] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
//                        updateResults(users, startTime)
//                    }
                    withContext(Dispatchers.Main) {
                        println("[CONCURRENT 9] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
                        updateResults(users, startTime)
                    }
                    val end = System.currentTimeMillis()
                    System.err.println("duration:" + (end - start))
                }.setUpCancellation()
                println("[CONCURRENT 10] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
            }

            NOT_CANCELLABLE -> { // Performing requests in a non-cancellable way
                println("[NOT_CANCELLABLE 1] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
                launch {
                    println("[NOT_CANCELLABLE 2] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
                    val users = loadContributorsNotCancellable(service, req)
                    println("[NOT_CANCELLABLE 8] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
                    updateResults(users, startTime)
                }.setUpCancellation()
                println("[NOT_CANCELLABLE 9] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
            }

            PROGRESS -> { // Showing progress
                launch(Dispatchers.Default) {
                    loadContributorsProgress(service, req) { users, completed ->
                        withContext(Dispatchers.Main) {
                            updateResults(users, startTime, completed)
                        }
                    }
                }.setUpCancellation()
            }

            CHANNELS -> {  // Performing requests concurrently and showing progress
                launch(Dispatchers.Default) {
                    loadContributorsChannels(service, req) { users, completed ->
                        withContext(Dispatchers.Main) {
                            updateResults(users, startTime, completed)
                        }
                    }
                }.setUpCancellation()
            }
        }
    }

    private enum class LoadingStatus { COMPLETED, CANCELED, IN_PROGRESS }

    private fun clearResults() {
        updateContributors(listOf())
        updateLoadingStatus(IN_PROGRESS)
        setActionsStatus(newLoadingEnabled = false)
    }

    private fun updateResults(
        users: List<User>,
        startTime: Long,
        completed: Boolean = true
    ) {
        println("[updateResults] thread name:" + Thread.currentThread().name + ",thread id=" + Thread.currentThread().id + "," + Date().toString())
        updateContributors(users)
        updateLoadingStatus(if (completed) COMPLETED else IN_PROGRESS, startTime)
        if (completed) {
            setActionsStatus(newLoadingEnabled = true)
        }
    }

    private fun updateLoadingStatus(
        status: LoadingStatus,
        startTime: Long? = null
    ) {
        val time = if (startTime != null) {
            val time = System.currentTimeMillis() - startTime
            "${(time / 1000)}.${time % 1000 / 100} sec"
        } else ""

        val text = "Loading status: " +
                when (status) {
                    COMPLETED -> "completed in $time"
                    IN_PROGRESS -> "in progress $time"
                    CANCELED -> "canceled"
                }
        setLoadingStatus(text, status == IN_PROGRESS)
    }

    private fun Job.setUpCancellation() {
        // make active the 'cancel' button
        setActionsStatus(newLoadingEnabled = false, cancellationEnabled = true)

        val loadingJob = this

        // cancel the loading job if the 'cancel' button was clicked
        val listener = ActionListener {
            loadingJob.cancel()
            updateLoadingStatus(CANCELED)
        }
        addCancelListener(listener)

        // update the status and remove the listener after the loading job is completed
        launch {
            loadingJob.join()
            setActionsStatus(newLoadingEnabled = true)
            removeCancelListener(listener)
        }
    }

    fun loadInitialParams() {
        setParams(loadStoredParams())
    }

    fun saveParams() {
        val params = getParams()
        if (params.username.isEmpty() && params.password.isEmpty()) {
            removeStoredParams()
        } else {
            saveParams(params)
        }
    }

    fun getSelectedVariant(): Variant

    fun updateContributors(users: List<User>)

    fun setLoadingStatus(text: String, iconRunning: Boolean)

    fun setActionsStatus(newLoadingEnabled: Boolean, cancellationEnabled: Boolean = false)

    fun addCancelListener(listener: ActionListener)

    fun removeCancelListener(listener: ActionListener)

    fun addLoadListener(listener: () -> Unit)

    fun addOnWindowClosingListener(listener: () -> Unit)

    fun setParams(params: Params)

    fun getParams(): Params
}
