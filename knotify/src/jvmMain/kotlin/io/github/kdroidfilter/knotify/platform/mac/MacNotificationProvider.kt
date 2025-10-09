package io.github.kdroidfilter.knotify.platform.mac

import com.sun.jna.Pointer
import io.github.kdroidfilter.knotify.builder.NotificationBuilder
import io.github.kdroidfilter.knotify.builder.NotificationProvider
import io.github.kdroidfilter.knotify.model.DismissalReason
import io.github.kdroidfilter.knotify.utils.extractToTempIfDifferent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import co.touchlab.kermit.Logger
import io.github.kdroidfilter.knotify.builder.NotificationInitializer

internal class MacNotificationProvider() : NotificationProvider {


    private val lib = MacNativeNotificationIntegration.INSTANCE
    private var coroutineScope: CoroutineScope? = null

    // Initialize Kermit logger
    private val logger = Logger.withTag("MacNotificationProvider")

    // Map to store notifications by their ID
    private val activeNotifications = mutableMapOf<Int, Pointer?>()

    override fun sendNotification(builder: NotificationBuilder) {
        coroutineScope = CoroutineScope(Dispatchers.IO).also { scope ->
            scope.launch {
                try {
                    val appIconPath = builder.smallIconPath ?: NotificationInitializer.appConfiguration.smallIcon
                    logger.d { "Sending notification with title: ${builder.title}" }

                    // Try to create the notification but handle any exceptions
                    val notification = try {
                        lib.create_notification(
                            title = builder.title,
                            body = builder.message,
                            iconPath = appIconPath
                        )
                    } catch (e: Exception) {
                        logger.e { "Exception creating notification: ${e.message}" }
                        null
                    }

                    if (notification == null) {
                        logger.e { "Failed to create notification." }
                        builder.onFailed?.invoke()
                        return@launch
                    }

                    // Store the notification pointer
                    activeNotifications[builder.id] = notification

                    try {
                        // Set up a clicked callback
                        builder.onActivated?.let { onActivated ->
                            val clickedCallback = object : NotificationClickedCallback {
                                override fun invoke(notification: Pointer?, userData: Pointer?) {
                                    onActivated()
                                }
                            }
                            lib.set_notification_clicked_callback(notification, clickedCallback, Pointer.NULL)
                        }

                        // Set up a closed callback
                        builder.onDismissed?.let { onDismissed ->
                            val closedCallback = object : NotificationClosedCallback {
                                override fun invoke(notification: Pointer?, userData: Pointer?) {
                                    onDismissed(DismissalReason.UserCanceled)
                                }
                            }
                            lib.set_notification_closed_callback(notification, closedCallback, Pointer.NULL)
                        }

                        // Add a large image if available
                        val largeImagePath = builder.largeImagePath
                        logger.d { "Large image path from builder: $largeImagePath" }

                        largeImagePath?.let { path: String ->
                            logger.d { "Processing large image path: $path" }
                            try {
                                // Check if the path is a file that exists directly
                                val file = File(path)
                                if (file.exists() && file.isFile) {
                                    logger.d { "Image file exists directly at: ${file.absolutePath}" }
                                    // Convert an absolute path to file URL
                                    val fileUrl = "file://${file.absolutePath}"
                                    logger.d { "Image file URL: $fileUrl" }
                                    lib.set_notification_image(notification, fileUrl)
                                    logger.d { "Notification image set successfully with direct path" }
                                } else {
                                    // Try to extract from resources if not a direct file
                                    val extractedFile = extractToTempIfDifferent(path)
                                    logger.d { "Extracted file: $extractedFile" }

                                    val largeImageAbsolutePath = extractedFile?.absolutePath
                                    logger.d { "Large image absolute path: $largeImageAbsolutePath" }

                                    largeImageAbsolutePath?.let { it: String ->
                                        // Convert an absolute path to file URL
                                        val fileUrl = "file://$it"
                                        logger.d { "Image file URL: $fileUrl" }
                                        lib.set_notification_image(notification, fileUrl)
                                        logger.d { "Notification image set successfully" }
                                    } ?: logger.e { "Failed to get absolute path for large image" }
                                }
                            } catch (e: Exception) {
                                logger.e { "Exception processing large image: ${e.message}" }
                            }
                        } ?: logger.d { "No large image path provided" }

                        // Add buttons
                        builder.buttons.forEach { button ->
                            val buttonCallback = object : ButtonClickedCallback {
                                override fun invoke(notification: Pointer?, buttonId: String?, userData: Pointer?) {
                                    button.onClick.invoke()
                                }
                            }
                            lib.add_button_to_notification(
                                notification = notification,
                                buttonId = button.label,
                                buttonLabel = button.label,
                                callback = buttonCallback,
                                userData = Pointer.NULL
                            )
                        }

                        // Send the notification but catch any exceptions
                        val result = try {
                            lib.send_notification(notification)
                        } catch (e: Exception) {
                            logger.e { "Exception sending notification: ${e.message}" }
                            // If we get an exception, consider it a failure
                            -1
                        }

                        if (result == 0) {
                            logger.i { "Notification sent successfully." }
                        } else {
                            logger.e { "Failed to send notification." }
                            builder.onFailed?.invoke()
                            activeNotifications.remove(builder.id)
                            try {
                                lib.cleanup_notification(notification)
                            } catch (e: Exception) {
                                logger.e { "Exception cleaning up notification: ${e.message}" }
                            }
                        }
                    } catch (e: Exception) {
                        logger.e { "Unexpected exception: ${e.message}" }
                        builder.onFailed?.invoke()
                        activeNotifications.remove(builder.id)
                        try {
                            lib.cleanup_notification(notification)
                        } catch (e: Exception) {
                            logger.e { "Exception cleaning up notification: ${e.message}" }
                        }
                    }
                } catch (e: Exception) {
                    logger.e { "Critical exception in sendNotification: ${e.message}" }
                    builder.onFailed?.invoke()
                }
            }
        }
    }

    override fun hideNotification(builder: NotificationBuilder) {
        try {
            val notification = activeNotifications[builder.id]
            if (notification != null) {
                try {
                    // First, try to hide the notification
                    lib.hide_notification(notification)
                    logger.d { "Notification hide called for ID: ${builder.id}" }

                    // Wait a short time to ensure the hide operation completes
                    Thread.sleep(100)

                    // Try to hide again to catch any pending notifications
                    lib.hide_notification(notification)
                    logger.d { "Second notification hide called for ID: ${builder.id}" }
                } catch (e: Exception) {
                    logger.e { "Exception hiding notification: ${e.message}" }
                }

                try {
                    lib.cleanup_notification(notification)
                } catch (e: Exception) {
                    logger.e { "Exception cleaning up notification: ${e.message}" }
                }

                activeNotifications.remove(builder.id)
                logger.d { "Notification hidden and cleaned up: ${builder.id}" }
            } else {
                logger.w { "No active notification found with ID: ${builder.id}" }
            }
        } catch (e: Exception) {
            logger.e { "Critical exception in hideNotification: ${e.message}" }
        }
    }
}
