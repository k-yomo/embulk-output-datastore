package org.embulk.output.datastore

import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.Timestamp
import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.datastore.Entity
import com.google.cloud.datastore.Key
import org.embulk.config.ConfigException
import org.embulk.config.TaskReport
import org.embulk.spi.*
import org.embulk.spi.type.Types
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.lang.NullPointerException

class DatastorePageOutput(
        private val task: DatastoreOutputPlugin.PluginTask,
        private val pageReader: PageReader,
        private val datastore: Datastore
) : TransactionalPageOutput {
    private var counter: Long = 0
    private val logger: Logger = LoggerFactory.getLogger("DatastorePageOutput")

    override fun add(page: Page?) {
        pageReader.setPage(page)
        val batch = datastore.newBatch()
        while (pageReader.nextRecord()) {
            val entity = Entity.newBuilder()
            var key: Key? = null
            pageReader.schema.columns.forEach {
                when (it.type) {
                    Types.BOOLEAN -> {
                        entity.set(it.name, pageReader.getBoolean(it))
                    }
                    Types.LONG -> {
                        val value = pageReader.getLong(it)
                        entity.set(it.name, value)
                        if (task.keyColumnName == it.name) {
                            key = Key.newBuilder(task.projectId, task.kind, value).build()
                        }
                    }
                    Types.DOUBLE -> {
                        entity.set(it.name, pageReader.getDouble(it))
                    }
                    Types.STRING -> {
                        val value = pageReader.getString(it)
                        entity.set(it.name, value)
                        if (task.keyColumnName == it.name) {
                            key = Key.newBuilder(task.projectId, task.kind, value).build()
                        }
                    }
                    Types.TIMESTAMP -> {
                        val ts = pageReader.getTimestamp(it)
                        // FIXME: java.lang.NoSuchMethodError: com.google.common.base.Preconditions.checkArgument(ZLjava/lang/String;JI)V
//                        entity.set(it.name, Timestamp.ofTimeSecondsAndNanos(ts.epochSecond, ts.nano))
                    }
                    else -> {
                        throw DataException("Unexpected column type:" + it.type)
                    }
                }
            }

            if (key != null) entity.setKey(key) else throw ConfigException("column corresponding to key not found")
            batch.put(entity.build())
            counter++
        }
        batch.submit()
    }

    override fun finish() {
        logger.info(String.format("Completed to write total %s records", counter));
    }

    override fun commit(): TaskReport {
        val report = Exec.newTaskReport()
        report["record_count"] = counter
        return report
    }

    override fun close() {
    }

    override fun abort() {
    }
}