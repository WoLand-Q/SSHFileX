package ua.ernest.sshfilex.work

import android.content.Context
import androidx.work.*
import ua.ernest.sshfilex.data.SshRepository
import java.io.File

class TransferWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val host      = inputData.getString(KEY_HOST)   ?: return Result.failure()
        val user      = inputData.getString(KEY_USER)   ?: return Result.failure()
        val port      = inputData.getInt(KEY_PORT, 22)
        val password  = inputData.getString(KEY_PASS)   ?: return Result.failure()
        val remote    = inputData.getString(KEY_REMOTE) ?: return Result.failure()
        val localPath = inputData.getString(KEY_LOCAL)  ?: return Result.failure()
        val localFile = File(localPath)

        val repo = SshRepository
        repo.connect(host, user, port, password).onFailure { return Result.retry() }
        return repo.download(remote, localFile).fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }

    companion object {
        const val KEY_HOST   = "host"
        const val KEY_USER   = "user"
        const val KEY_PORT   = "port"
        const val KEY_PASS   = "password"
        const val KEY_REMOTE = "remote"
        const val KEY_LOCAL  = "local"
    }
}
