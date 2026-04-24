# Keep WorkManager workers (instantiated by name)
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.Worker { *; }
