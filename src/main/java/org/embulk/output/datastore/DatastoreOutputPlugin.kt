package org.embulk.output.datastore

import com.google.common.base.Optional
import org.embulk.config.*
import org.embulk.spi.Exec
import org.embulk.spi.OutputPlugin
import org.embulk.spi.Schema
import org.embulk.spi.TransactionalPageOutput

class DatastoreOutputPlugin : OutputPlugin {
    interface PluginTask : Task {
        // configuration option 1 (required integer)
        @get:Config("option1")
        val option1: Int

        // configuration option 2 (optional string, null is not allowed)
        @get:ConfigDefault("\"myvalue\"")
        @get:Config("option2")
        val option2: String?

        // configuration option 3 (optional string, null is allowed)
        @get:ConfigDefault("null")
        @get:Config("option3")
        val option3: Optional<String?>?
    }

    override fun transaction(config: ConfigSource,
                             schema: Schema, taskCount: Int,
                             control: OutputPlugin.Control): ConfigDiff {
        val task = config.loadConfig(PluginTask::class.java)
        // retryable (idempotent) output:
// return resume(task.dump(), schema, taskCount, control);
// non-retryable (non-idempotent) output:
        control.run(task.dump())
        return Exec.newConfigDiff()
    }

    override fun resume(taskSource: TaskSource,
                        schema: Schema, taskCount: Int,
                        control: OutputPlugin.Control): ConfigDiff {
        throw UnsupportedOperationException("datastore output plugin does not support resuming")
    }

    override fun cleanup(taskSource: TaskSource,
                         schema: Schema, taskCount: Int,
                         successTaskReports: List<TaskReport>) {
    }

    override fun open(taskSource: TaskSource, schema: Schema, taskIndex: Int): TransactionalPageOutput {
        val task = taskSource.loadTask(PluginTask::class.java)
        throw UnsupportedOperationException("DatastoreOutputPlugin.run method is not implemented yet")
    }
}