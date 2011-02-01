import org.rioproject.config.Constants
import java.util.logging.Level

import org.rioproject.resources.servicecore.Service

deployment(name:'directoryNodeService',  debug: 'true') {

    logging {
        logger 'org.rioproject.resolver', Level.ALL
        logger 'org.rioproject.associsations' , Level.ALL
    }
    /* Configuration for the discovery group that the service should join.
     * This first checks if the org.rioproject.groups property is set, if not
     * the user name is used */
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))

    /* Declares the artifacts required for deployment. Note the 'dl'
     * classifier used for the 'download' jar */
    artifact id:'api', 'org.drools:drools-grid-distributed-api:5.2.0.M1'
    artifact id:'service-dir', 'org.drools:drools-grid-distributed-dir-rio:5.2.0.M1'
    artifact id:'service-dir-dl', 'org.drools:drools-grid-distributed-dir-rio:dl:5.2.0.M1'
    artifact id:'service', 'org.drools:drools-grid-distributed-rio:5.2.0.M1'
    artifact id:'service-dl', 'org.drools:drools-grid-distributed-rio:dl:5.2.0.M1'

    /*
     * Declare the service to be deployed. The number of instances deployed
     * defaults to 1. If you require > 1 instances change as needed
     */

    service(name: 'DirectoryNodeService') {
        interfaces {
            classes 'org.drools.grid.DirectoryNodeService'
            artifact ref:'service-dir-dl'
        }
        implementation(class:'org.drools.distributed.directory.impl.DirectoryNodeServiceImpl') {
            artifact ref:'service-dir'
        }

        association (name:'ExecutionNodeService', type:'uses', property:'executionNodes')

        maintain 1
    }

     service(name: 'ExecutionNodeService', fork: 'no') { //fork yes, works only in unix machines
        interfaces {
            classes 'org.drools.grid.ExecutionNodeService'
            artifact ref:'service-dl'
        }
        implementation(class:'org.drools.grid.distributed.impl.ExecutionNodeServiceImpl') {
            artifact ref:'service'
        }

        maintain 1
    }

    

}
