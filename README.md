# Elasticsearch Client

Jahia module that provides commands to administrate an Elasticsearch server to which your Jahia server is connected
through the `database-connector` & `elasticsearch-connector-7` modules.

## <a name="how-to-use"></a>How to use?
### <a name="es-connection"></a>es:connection
Displays the available connections, and allow to select the one to use.

**Examples:**

    jahia@dx()> es:connection
    Connection   | Selected
    ---------------------
    jahia-as     |
    jahia-jexp   | 

    jahia@dx()> es:connection jahia-as
    Client loaded for the connection jahia-as

    jahia@dx()> es:connection
    Connection   | Selected
    ---------------------
    jahia-as     | true
    jahia-jexp   |

### <a name="es-indices-list"></a>es:indices-list
Displays the indices available in the ES server.

**Examples:**

    jahia@dx()> es:indices-list
    Index                       | Aliases
    ----------------------------------------------------------------
    attribute_2021-04-14_130220 | attribute_alias
    dx_v2__de__1617177774968    | dx_v2__de__read , dx_v2__de__write
    family_2021-04-14_130220    | family_alias
    dx_v2__en__1617177774968    | dx_v2__en__read , dx_v2__en__write
    dx_v2__fr__1617177774968    | dx_v2__fr__read , dx_v2__fr__write
    product_2021-04-14_130220   | product_alias  

### <a name="es-indices-delete"></a>es:indices-delete
Deletes one or several indices.

**Examples:**                  
    
Deletion of one index:

    jahia@dx()> es:indices-delete attribute_2021-04-14_130220
    Deleted attribute_2021-04-14_130220

Deletion of several indices:

    jahia@dx()> es:indices-delete attribute_2021-04-14_130220 family_2021-04-14_130220
    Deleted attribute_2021-04-14_130220
    Deleted family_2021-04-14_130220

Deletion of several indices using a wildcard:

    jahia@dx()> es:indices-delete dx*
    Deleted dx_v2__de__1617177774968
    Deleted dx_v2__en__1617177774968
    Deleted dx_v2__fr__1617177774968

Deletion of all indices:

    jahia@dx()> es:indices-delete *
    Deleted dx_v2__de__1617177774968
    Deleted dx_v2__en__1617177774968
    Deleted dx_v2__fr__1617177774968  
    Deleted attribute_2021-04-14_130220
    Deleted family_2021-04-14_130220
    Deleted product_2021-04-14_130220 

### <a name="es-cluster-health"></a>es:cluster-health
Displays the ES cluster health information.

**Examples:**

    jahia@dx()> es:cluster-health
    Info                      | Values
    ------------------------------------
    Cluster name              | jahia-as
    Status                    | YELLOW
    Number Of Nodes           | 1
    Number Of Data Nodes      | 1
    REST Status               | OK
    is Timed Out              | false
    Active Shards             | 6
    Active Primary Shards     | 6
    Active Shards Percent     | 50.0
    Delayed Unassigned Shards | 0
    Initializing Shards       | 0
    Relocating Shards         | 0
    Unassigned Shards         | 6
    Number Of In Flight Fetch | 0
    Number Of Pending Tasks   | 0
    Task Max Waiting Time     | 0s

## <a name="faq"></a>FAQ
### <a name="faq-tools"></a>How to use from the tools?
The commands rely on the Karaf session to store the current connection. As a consequence, it is 
required to chain the commands in a single request when running them from the Jahia Tools.

**Examples:**

    es:connection jahia-as ; es:indices-list    
    es:connection jahia-as ; es:indices-delete *   
    es:connection jahia-as ; es:indices-cluster-health   