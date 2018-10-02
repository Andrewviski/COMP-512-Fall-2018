# comp512-project

To use the system please use the following scripts.

## Full system scripts

### Start the whole system

This script will launch the four required servers then launch a middler and connect it to them. `[local]` will indicate if the system is hosted locally, if not the script will distribute the system over 5 hardcoded hosts in it.

```[bash]
./run_system.sh [local]
```

Note: this script uses the following scripts to do its work, so it would have similar side effects (it might kill processes).

### Clear ports [54000 to 54006]

```[bash]
./clear_ports.sh
```

### Rebuild the system

```[bash]
./make_all.sh
```

---

## Server side

### Starting a server on a specific port

```[bash]
cd Server/
./run_server.sh <port> <name>
```

Note: this script will kill any process listening on the specified port before launching the server.

---

## Middleware side

### Starting middleware on port 54006

```[bash]
cd Middlware/
./run_middleware.sh
[<hostname of Flights server>='localhost']
[<hostname of Rooms server>='localhost']
[<hostname of Cars server>='localhost]
[<hostname of Customers server>='localhost]
[<port of Flights server>=54002]
[<port of Rooms server>=54003]
[<port of Cars server>=54004]
[<port of Customers server>=54005]
```

Note: this script will kill any process listening on port 54006 before launching the middlware.

---

## Client side

### Starting a standerd client to connect to middleware

```[bash]
cd Client/
./run_client.sh
[<middleware_hostname>='localhost']
[<middleware_port>=54006]
```

### Starting an automated testing client conneted to middleware

```[bash]
cd Client/
./run_teseter_client.sh
[<middleware_hostname>='localhost']
[<middleware_port>=54006]
```

---

## Notes

````[text]
[<x>=v]: optional parameter with default value v.
<x>: required parameter.
```