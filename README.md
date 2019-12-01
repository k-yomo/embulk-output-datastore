# embulk-output-datastor

[Embulk](https://github.com/embulk/embulk/) output plugin to put data into [Cloud Firestore(Datastore mode)](https://cloud.google.com/firestore/)

## Overview

* **Plugin type**: output
* **Load all or nothing**: no
* **Resume supported**: no
* **Cleanup supported**: no

## Configuration

| name                                 | type        | required?  | default                  | description            |
|:-------------------------------------|:------------|:-----------|:-------------------------|:-----------------------|
|  auth_method                         | string      | optional   | "application\_default"   | See [Authentication](#authentication) |
|  json_keyfile                        | string      | optional   |                          | google credentials jsont  |
|  project_id                          | string      | required   |                          | project\_id |
|  kind                                | string      | required   |                          | entity's kind  |
|  key_column_name                     | string      | optional   | id                       | column name corresponding to key |

## Example

```yaml
out:
  type: datastore
  project_id: test_project
  kind: User
  key_column_name: id
  json_keyfile: '{"client_id":"xxxxxxxxxxx.apps.googleusercontent.com","client_secret":"xxxxxxxxxxx","refresh_token":"xxxxxxxxxxx","type":"authorized_user"}'
```

### Authentication

There are four authentication methods

1. `service_account`
1. `authorized_user`
1. `compute_engine`
1. `application_default`

#### service\_account

Use GCP service account credentials.
You first need to create a service account, download its json key and deploy the key with embulk.

```yaml
out:
  type: datastore
  auth_method: service_account
  json_keyfile: '{"private_key_id": "123456789","private_key": "-----BEGIN PRIVATE KEY-----\nABCDEF","client_email": "..."}'
```

#### authorized\_user

Use Google user credentials.
You can get your credentials at `~/.config/gcloud/application_default_credentials.json` by running `gcloud auth login`.

```yaml
out:
  type: datastore
  auth_method: authorized_user
  json_keyfile: '{"client_id":"xxxxxxxxxxx.apps.googleusercontent.com","client_secret":"xxxxxxxxxxx","refresh_token":"xxxxxxxxxxx","type":"authorized_user"}'
```

#### compute\_engine

On the other hand, you don't need to explicitly create a service account for embulk when you
run embulk in Google Compute Engine. In this third authentication method, you need to
add the API scope "https://www.googleapis.com/auth/datastore" to the scope list of your
Compute Engine VM instance, then you can configure embulk like this.

```yaml
out:
  type: datastore
  auth_method: compute_engine
```

#### application\_default

Use Application Default Credentials (ADC). ADC is a strategy to locate Google Cloud Service Account credentials.

1. ADC checks to see if the environment variable `GOOGLE_APPLICATION_CREDENTIALS` is set. If the variable is set, ADC uses the service account file that the variable points to.
2. ADC checks to see if `~/.config/gcloud/application_default_credentials.json` is located. This file is created by running `gcloud auth application-default login`.
3. Use the default service account for credentials if the application running on Compute Engine, App Engine, Kubernetes Engine, Cloud Functions or Cloud Run.

See https://cloud.google.com/docs/authentication/production for details.

```yaml
out:
  type: datastore
  auth_method: application_default
```

## Build

```
$ ./gradlew gem  # -t to watch change of files and rebuild continuously
```
