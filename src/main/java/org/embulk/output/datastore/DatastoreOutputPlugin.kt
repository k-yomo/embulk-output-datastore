package org.embulk.output.datastore

import com.google.api.gax.core.CredentialsProvider
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.auth.Credentials
import com.google.auth.oauth2.*
import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.DatastoreOptions
import com.google.common.base.Optional
import io.grpc.ManagedChannelBuilder
import org.embulk.config.*
import org.embulk.spi.*
import java.io.ByteArrayInputStream


class DatastoreOutputPlugin : OutputPlugin {
    interface PluginTask : Task {
        @get:Config("project_id")
        val projectId: String

        @get:ConfigDefault("application_default")
        @get:Config("auth_method")
        val authMethod: String?

        @get:ConfigDefault("null")
        @get:Config("json_keyfile")
        val jsonKeyfile: Optional<String?>?

        @get:Config("kind")
        val kind: String

        @get:Config("key_column_name")
        val keyColumnName: String
    }

    override fun transaction(config: ConfigSource, schema: Schema, taskCount: Int, control: OutputPlugin.Control): ConfigDiff {
        val task = config.loadConfig(PluginTask::class.java)
        return resume(task.dump(), schema, taskCount, control)
    }

    override fun resume(taskSource: TaskSource,
                        schema: Schema, taskCount: Int,
                        control: OutputPlugin.Control): ConfigDiff {
        control.run(taskSource)
        return Exec.newConfigDiff()
    }

    override fun cleanup(taskSource: TaskSource,
                         schema: Schema, taskCount: Int,
                         successTaskReports: List<TaskReport>) {
    }

    override fun open(taskSource: TaskSource, schema: Schema, taskIndex: Int): TransactionalPageOutput {
        val task = taskSource.loadTask(PluginTask::class.java)
        val pageReader = PageReader(schema)
        val datastore = initializeDatastoreClient(task.projectId, task.authMethod, task.jsonKeyfile)
        return DatastorePageOutput(task, pageReader, datastore)
    }

    private fun initializeDatastoreClient(projectId: String, authMethod: String?, jsonKeyFile: Optional<String?>?): Datastore {
        return DatastoreOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(authenticate(authMethod, jsonKeyFile))
                .build()
                .service
    }

    private fun authenticate(authMethod: String?, jsonKeyFile: Optional<String?>?): Credentials {
        when (authMethod) {
            "authorized_user" -> {
                // TODO: パスからも設定できるようにする
                return ServiceAccountCredentials.fromStream(
                        ByteArrayInputStream(jsonKeyFile?.get()?.toByteArray())
                )
            }
            "compute_engine" -> {
                return ComputeEngineCredentials.getApplicationDefault()
            }
            "service_account" -> {
                FixedCredentialsProvider.create(
                        // TODO: パスからも設定できるようにする
                        // FileInputStream(task.jsonKeyfile.toString())
                        ServiceAccountCredentials.fromStream(
                                ByteArrayInputStream(jsonKeyFile?.get()?.toByteArray())
                        )
                ).credentials!!
            }
            "application_default" -> {
                return GoogleCredentials.getApplicationDefault()
            }
        }
        return NoCredentialsProvider().credentials
    }
}