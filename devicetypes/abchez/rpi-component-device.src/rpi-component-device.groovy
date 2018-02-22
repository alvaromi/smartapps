/**
 *  RPI Component Device
 *
 *  Copyright 2018 Alvaro Miranda
 *
 */
metadata {
	definition (name: "RPI Component Device", namespace: "abchez", author: "Alvaro Miranda") {

	}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
		// TODO: define your main and details tiles here
	}
}

// parse events into attributes
def parse(String description) {
	def msg = parseLanMessage(description)
	parent.Log "====> Parsing ${msg}"
}

def refreshChildren(rpiChildDevices) {
    def parentId = device.getDeviceNetworkId()
    def latestDevices = rpiChildDevices.each { d -> parentId + ":" + d.key }
        
    def delete = getChildDevices().findAll { d -> !latestDevices.Contains(d.getDeviceNetworkId()) }
    delete.each { d -> deleteChildDevice(d.getDeviceNetworkId()) }
    
    rpiChildDevices.each { d ->
        def childId = parentId + ":" + d.key
        def piDevice = getChildDevices()?.find { existing -> existing.getDeviceNetworkId() == childId }
        if (!piDevice) {
            parent.Log "Creating RPI Device with dni: ${childId}"
            //piDevice = addChildDevice("abchez", "RPI Motion Device", childId, hubId)
        }
    }
    
    return rpiChildDevices

}