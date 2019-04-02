# Rendezvous
System consist of 2 modules:
* Proxy
* Model

## Architecture
![alt text](diagrams/ArchitectureDiagram.png)

## Components
### Proxy
Proxy receives requests from user over REST or gRPC. Then it sends requests over MapR Streams with models and waiting for responses. When responses are coming we look if they are from primary/requested model or not, if we have not received response from primary/requested model in timeout, than we will choose response from model, which have more accuracy. Proxy is also listening for discovery topic and writes info about changes in models to MapR DB. List of models and their accuracy is also available via REST.

### Model
At start model publishes info about itself to discovery topic in MapR Streams. After configuration model subscribes for task's topic in MapR Streams. When model receives request it slips some time for emulating computing and sends result to result topic in MapR Streams.

### Communication between components
![alt text](diagrams/MapRStreamsDiagram.png)

