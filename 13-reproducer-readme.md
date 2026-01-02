1. Launch server 1:

```
reset; docker run -v $(pwd):/user-config  -p 11222:11222 -e USER=admin -e PASS=password -it quay.io/infinispan/server:13.0.21.Final -c /user-config/config-13.xml;
```

2. Launch server 2:

```
reset; docker run -v $(pwd):/user-config  -p 11223:11222 -e USER=admin -e PASS=password -it quay.io/infinispan/server:13.0.21.Final -c /user-config/config-13-zero.xml
```
