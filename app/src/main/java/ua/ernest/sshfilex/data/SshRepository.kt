// file: app/src/main/java/ua/ernest/sshfilex/data/SshRepository.kt
package ua.ernest.sshfilex.data

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties

/**
 * Простая обёртка над JSch для SFTP.
 */
data class RemoteFile(val name: String, val size: Long, val isDir: Boolean)

object SshRepository {
    // Держим одну сессию на всё приложение
    private var session: Session? = null

    /**
     * Устанавливает соединение с сервером.
     * Вызывается при логине.
     */
    suspend fun connect(host: String, user: String, port: Int, pass: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                session?.disconnect()
                session = JSch().getSession(user, host, port).apply {
                    setPassword(pass)
                    // чтобы не требовать known_hosts
                    setConfig("StrictHostKeyChecking", "no")
                    connect(10_000)
                }
            }
        }

    /**
     * Закрывает сессию.
     */
    fun disconnect() {
        session?.disconnect()
        session = null
    }

    /**
     * Возвращает список файлов по пути на сервере.
     */
    suspend fun list(path: String): Result<List<RemoteFile>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val sftp = openSftp()
                val entries = sftp.ls(path)
                    .filterIsInstance<ChannelSftp.LsEntry>()
                    // убираем . и ..
                    .filter { it.filename != "." && it.filename != ".." }
                    .map {
                        RemoteFile(
                            name = it.filename,
                            size = it.attrs.size,
                            isDir = it.attrs.isDir
                        )
                    }
                sftp.disconnect()
                entries
            }
        }

    /**
     * Скачивает файл с сервера в локальный File.
     */
    suspend fun download(remote: String, local: File): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val sftp = openSftp()
                sftp.get(remote, local.absolutePath)
                sftp.disconnect()
            }
        }

    /**
     * Скачивает файл с сервера в любой OutputStream (например для SAF).
     */
    suspend fun download(remote: String, out: OutputStream): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val sftp = openSftp()
                sftp.get(remote, out)
                out.close()
                sftp.disconnect()
            }
        }

    /**
     * **Новый метод**: загружает данные на сервер из переданного InputStream.
     */
    suspend fun upload(remote: String, input: InputStream): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val sftp = openSftp()
                // передаём InputStream прямо в SFTP
                sftp.put(input, remote)
                sftp.disconnect()
            }
        }

    /**
     * Вспомогательный метод: открывает SFTP-канал.
     */
    private fun openSftp(): ChannelSftp {
        val sess = session ?: error("SFTP session is not initialized. Call connect(...) first.")
        val chan = sess.openChannel("sftp")
        chan.connect()
        @Suppress("UNCHECKED_CAST")
        return chan as ChannelSftp
    }
}
