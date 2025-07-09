package ua.ernest.sshfilex.work
import androidx.work.WorkRequest
import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Упрощённая обёртка: создаём и ставим OneTimeWorkRequest на загрузку файла.
 *
 * @param ctx      любой Context (Activity / Application)
 * @param host     SSH-хост
 * @param user     SSH-пользователь
 * @param port     SSH-порт (22)
 * @param password Пароль
 * @param remote   Путь к удалённому файлу
 * @param local    Полный путь, куда сохранить на устройстве
 */
fun enqueueDownload(
    ctx: Context,
    host: String,
    user: String,
    port: Int = 22,
    password: String,
    remote: String,
    local: String
) {
    val data = workDataOf(
        TransferWorker.KEY_HOST   to host,
        TransferWorker.KEY_USER   to user,
        TransferWorker.KEY_PORT   to port,
        TransferWorker.KEY_PASS   to password,
        TransferWorker.KEY_REMOTE to remote,
        TransferWorker.KEY_LOCAL  to local
    )

    val req = OneTimeWorkRequestBuilder<TransferWorker>()
        .setInputData(data)
        .setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            WorkRequest.MIN_BACKOFF_MILLIS,
            TimeUnit.MILLISECONDS
        )
        .build()

    WorkManager.getInstance(ctx).enqueueUniqueWork(
        /* uniqueName = */ "download_$remote",
        ExistingWorkPolicy.REPLACE,
        req
    )
}
