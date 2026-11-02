package com.example.githubmovil.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.githubmovil.R
import com.example.githubmovil.data.GitHubClient
import com.example.githubmovil.data.GitHubException
import com.example.githubmovil.data.PullRequest
import com.example.githubmovil.data.TokenStore
import com.example.githubmovil.presentation.MainActivity
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Revisión en segundo plano: cada 15 min (el mínimo que permite Android) consulta GitHub
 * y manda una notificación al reloj por cada PR nuevo. Funciona con la app cerrada
 * y sobrevive a reinicios del reloj (WorkManager lo reprograma solo).
 */
class PrCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val store = TokenStore(applicationContext)
        val token = store.token ?: return Result.success()

        val prs = try {
            GitHubClient.openPullRequests(token)
        } catch (e: GitHubException) {
            return if (e.code == 401) Result.success() else Result.retry()
        } catch (e: IOException) {
            return Result.retry()
        }

        val seen = store.seenPrs
        store.seenPrs = prs.map { it.key }.toSet()

        // La primera sincronización solo memoriza lo que ya existe, sin avisar.
        if (seen != null) {
            val fresh = prs.filter { it.key !in seen && !it.draft }
            PrNotifier.notifyNewPrs(applicationContext, fresh)
        }
        return Result.success()
    }
}

object PrCheckScheduler {
    private const val WORK_NAME = "pr-check"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<PrCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

object PrNotifier {
    private const val CHANNEL_ID = "new_prs"
    private const val GROUP = "com.example.githubmovil.NEW_PRS"
    const val EXTRA_PR_KEY = "open_pr_key"

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Nuevos Pull Requests",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Aviso cuando aparece un PR nuevo en tus repositorios"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 120, 80, 120)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission") // comprobado en canNotify()
    fun notifyNewPrs(context: Context, prs: List<PullRequest>) {
        if (prs.isEmpty() || !canNotify(context)) return
        ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)

        prs.take(5).forEach { pr ->
            val intent = Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_PR_KEY, pr.key)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pending = PendingIntent.getActivity(
                context,
                pr.key.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_pr)
                .setColor(0xFF8957E5.toInt())
                .setContentTitle("Nuevo PR · ${pr.repoFullName.substringAfter('/')}")
                .setContentText(pr.title)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("${pr.title}\n#${pr.number} por ${pr.author}\n${pr.headBranch} → ${pr.baseBranch}")
                )
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setGroup(GROUP)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .addAction(R.drawable.ic_stat_pr, "Revisar", pending)
                .build()
            manager.notify(pr.key.hashCode(), notification)
        }
    }
}
