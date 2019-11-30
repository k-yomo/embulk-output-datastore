Embulk::JavaPlugin.register_output(
  "datastore", "org.embulk.output.datastore.DatastoreOutputPlugin",
  File.expand_path('../../../../classpath', __FILE__))
