# Using The Java OpenLiberty Stack, Kafka, and MP Reactive Messaging

The following instructions show how to deploy an application implementation using the Java OpenLiberty Stack. The application
makes use of MicroProfile reactive messaging and Kafka.

## About The Application

The Application consist of two endpoints: system and inventory.
The system endpoint calculates and publishes system load events to a topic. The inventory endpoint subscribes to the same topic to listen for system load updates. The updates are automatically posted by the system endpoint every 15 minutes.

## Inner Loop Deployment

### Pre-reqs

- Openshift cluster.
- Strimzy Operator.
- ODO v2.0.3+.

### Procedure

1. Create a new project and make it the default.

```
odo project create myproject
```

2. Clone the repository.

```
git clone git@github.com:OpenLiberty/application-stack-samples.git
```
```
cd kafka
```

3. Create a Kafka resource instance.

```
kubectl apply -f kafka.yaml
```

4. Create an ODO component using the java-OpenLiberty stack.

- Create the java-openliberty stack component.

```
odo create java-openliberty reactiveapp
```

- Create an environment variable that defines the kafka botstrapServers to connect to.

```
KAFKA_BOOTSTRAP_SERVERS=$(kubectl get Kafka kafka-cluster --output=jsonpath={.status.listeners[0].bootstrapServers})
```

```
odo config set --env mp.messaging.connector.liberty-kafka.bootstrap.servers=$KAFKA_BOOTSTRAP_SERVERS
```

- Create a route with target port 9085 for the external connection to access the application on that port.

```
odo url create reactiveapp-url --port 9085
```
Workaround:

```
vi .odo/env/env.yaml
```

Add `Port: 9085` to the Url section with name `reactiveapp-url`

Example:

```
  Url:
  - Name: reactiveapp-url
    Port: 9085
    Kind: route
```

- Deploy the application.

```
odo push
```

5. Get the route to access the application externally.

```
kubectl get routes -l odo.openshift.io/url-name=reactiveapp-url
```

Output:

```
kubectl get routes -l odo.openshift.io/url-name=reactiveapp-url
NAME                          HOST/PORT                                                            PATH   SERVICES      PORT   TERMINATION   WILDCARD
reactiveapp-url-reactiveapp   reactiveapp-url-reactiveapp-myproject.apps.my.cp.cluster.org.com   /      reactiveapp   9085                 None
```

Example:

http://reactiveapp-url-reactiveapp-myproject.apps.my.cp.cluster.org.com/health

Output:

```
checks	
0	
data	{}
name	"SystemLivenessCheck"
status	"UP"
1	
data	{}
name	"InventoryReadinessCheck"
status	"UP"
2	
data	{}
name	"InventoryLivenessCheck"
status	"UP"
3	
data	{}
name	"SystemReadinessCheck"
status	"UP"
status	"UP"
```

http://reactiveapp-url-reactiveapp-myproject.apps.my.cp.cluster.org.com/inventory/systems

Output:

```
0	
hostname	"reactiveapp-66c74cd97d-rkcz8"
systemLoad	1.37
```

## Manual Outer Loop Deployment

### Pre-reqs

- Openshift cluster.
- OpenLiberty Operator.
- Strimzy Operator.
- ODO v2.0.3.

### Procedure

1. Download the outer loop files.

```
curl -L https://github.com/OpenLiberty/application-stack/releases/download/outer-loop-0.4.0/app-deploy.yaml --output app-deploy.yaml
```
```
curl -L https://github.com/OpenLiberty/application-stack/releases/download/outer-loop-0.4.0/Dockerfile --output Dockerfile
```

2. Build the application image. 

```
docker build -t <docker-id>/kafka-reactive-outer-loop:0.0.1 .
```

3. Push the application image to your repository.

```
docker push <docker-id>/kafka-reactive-outer-loop:0.0.1
```

4. Update the app-deployment.yaml.

- Update GO template entries.

```
{{.CONTAINER_IMAGE}} = <docker-id>/kafka-reactive-outer-loop:0.0.1 (The image name we just created/pushed previously)
{{.COMPONENT_NAME}} = ola-reactiveapp (The name you would like this OpenLibertyApplication resource to have)
{{.PORT}} = 9085 (The port the application will listen on)
```

- Add an environment variable that defines the kafka botstrapServers to connect to.

`Name:` mp.messaging.connector.liberty-kafka.bootstrap.servers

`Value:` The output of command: kubectl get Kafka kafka-cluster --output=jsonpath={.status.listeners[0].bootstrapServers}

Example:

```
kind: OpenLibertyApplication
metadata:
  name: ola-reactiveapp
spec:
  version: 1.0.0
  applicationImage: <docker-id>/kafka-reactive-outer-loop:0.0.1
  env:
    - name: mp.messaging.connector.liberty-kafka.bootstrap.servers
      value: kafka-cluster-kafka-bootstrap.myproject.svc:9092
```


5. Deploy the OpenLibertyApplication resource.

```
kubectl apply -f app-deploy.yaml
```

6. Get the route to access the application externally and validate deployment.

```
kubectl get routes -l app.kubernetes.io/name=ola-reactiveapp
```

Output:

```
kubectl get routes -l app.kubernetes.io/name=ola-reactiveapp
NAME              HOST/PORT                                                PATH   SERVICES          PORT       TERMINATION   WILDCARD
ola-reactiveapp   ola-reactiveapp-myproject.apps.my.cp.cluster.org.com          ola-reactiveapp   9085-tcp                 None
```

Example:

http://ola-reactiveapp-myproject.apps.my.cp.cluster.org.com/health

Output:

```
checks	
0	
data	{}
name	"SystemLivenessCheck"
status	"UP"
1	
data	{}
name	"InventoryReadinessCheck"
status	"UP"
2	
data	{}
name	"InventoryLivenessCheck"
status	"UP"
3	
data	{}
name	"SystemReadinessCheck"
status	"UP"
status	"UP"
```

http://ola-reactiveapp-myproject.apps.my.cp.cluster.org.com/inventory/systems

Output:

```	
0	
hostname	"ola-reactiveapp-847b874cf-zlv7d"
systemLoad	1.1
```
