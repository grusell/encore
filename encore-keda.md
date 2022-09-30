# Encore keda poc

POC for running encore jobs with KEDA. Encore jobs will be enqueued on the redis queue like always,
but instead of the encore instance starting jobs from the queue, KEDA polls the queue and creates
Kubernetes Jobs.

Two spring profiles have been added:

singlejobworker - Takes a single job from the queue and executes it. This is the profile used
for the KubernetesJobs started by KEDA.

conductor - Handles rest endpoints and enqueing jobs.

If encore is run with none of the above profiles it works as usual.


# Example keda scaled job definition

```
apiVersion: keda.sh/v1alpha1
kind: ScaledJob
metadata:
  name: encore-keda-scaled-job-1
spec:
  jobTargetRef:
    parallelism: 1
    completions: 1
    activeDeadlineSeconds: 86400
    backoffLimit: 0
    template:
      metadata:
        labels:
          app: encore-keda-job
      spec:
        containers:
        - image: MY_ENCORE_DOCKER_IMAGE
          imagePullPolicy: Always
          name: encore-keda-job
          env:
          - name: SERVICE_NAME
            value: encore-with-keda-job
          - name: ENCORE_SETTINGS_REDIS_KEY_PREFIX
            value: MY_REDIS_KEY_PREFIX
          - name: ENCORE_SETTINGS_WORKER_QUEUE_NO
            value: "2"
          - name: SPRING_PROFILES_ACTIVE
            value: "singlejobworker"
          envFrom:
          - configMapRef:
              name: MY_CONFIG_MAP
          - secretRef:
              name: MY_SECRETS
          resources:
            limits:
              cpu: 10
              memory: 10Gi
            requests:
              cpu: 10
              memory: 10Gi
          volumeMounts:
          - mountPath: /my-mounts
            name: my-mounts
            subPath: bla-bla
        restartPolicy: Never
        volumes:
        - name: my-mounts
          nfs:
            path: /blabla
            server: MY_NFS_SERVER
                
  pollingInterval: 10                         # Optional. Default: 30 seconds
  successfulJobsHistoryLimit: 5              # Optional. Default: 100. How many completed jobs should be kept.
  failedJobsHistoryLimit: 10                   # Optional. Default: 100. How many failed jobs should be kept.
  maxReplicaCount: 5                       # Optional. Default: 100
  scalingStrategy:
    strategy: "default"                        # Optional. Default: default. Which Scaling Strategy to use. 
  triggers:
  - type: redis
    metadata:
      address: MY_REDIS_ADDRESS
      passwordFromEnv: REDIS_PASSWORD_ENV_VARIABLE
      listName: MY_REDIS_QUEUE_NAME
      listLength: "1" # Required
      enableTLS: "false" # optional
      databaseIndex: "3"
```
