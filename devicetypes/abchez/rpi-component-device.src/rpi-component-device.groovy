/**
 *  RPI Component Device
 *
 *  Copyright 2018 Alvaro Miranda
 *
 */
metadata {
	definition (name: "RPI Component Device", namespace: "abchez", author: "Alvaro Miranda") {
        capability "Bridge"
        attribute "state", "string"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {
        standardTile("stateTile", "device.state", width: 6, height: 4) {
            state "offline", label: '${currentValue}', icon: "st.Lighting.light99-hue", backgroundColor: "#ffffff"
            state "online", label: '${currentValue}', icon: "st.Lighting.light99-hue", backgroundColor: "#00a0dc"
        }
        valueTile("idTile", "device.id", width: 6, height: 2) {
              state "id", label:'MAC Addr: ${currentValue}'
        }
        main "stateTile"
    	details(["stateTile","idTile"])
	}
}

def getRpiDeviceTypeToDeviceHandler(String rpiDeviceType) {
    if (!state.rpiTypeToHandler) {
        state.rpiTypeToHandler = [
            'motion' : "RPI Motion Sensor Device",
            'contact' :  "RPI Contact Sensor Device",
        ]
    }
    return state.rpiTypeToHandler[rpiDeviceType];
}

def getDeviceHandlerToParseHandler(String deviceHandlerName) {
    if (!state.deviceHandlerToParseHandler) {
        state.deviceHandlerToParseHandler = [
            'RPI Motion Sensor Device' : { childDevice, rpiNotification ->
                    Log("parse for type RPI Motion Sensor Device")
                    def data = [ eventTime : rpiNotification.eventTime]
                    if (rpiNotification.state == "active") {
                        childDevice.setActive(data)
                    }
                    else if (rpiNotification.state == "inactive") {
                        childDevice.setInactive(data)
                    }
            },
            'RPI Contact Sensor Device' : { childDevice, rpiNotification ->
                    Log("parse for type RPI Contact Sensor Device")
                    def data = [ eventTime : rpiNotification.eventTime]
                    if (rpiNotification.state == "open") {
                        childDevice.setOpen(data)
                    }
                    else if (rpiNotification.state == "closed") {
                        childDevice.setClosed(data)
                    }
            }
        ]
    }
    return state.deviceHandlerToParseHandler[deviceHandlerName];
}

// parse events into attributes
def parse(String description) {
logEx {
    parent.Log("parse ${description}")
	def msg = parseLanMessage(description)
//    def headersAsString = msg.header // => headers as a string
//    def headerMap = msg.headers      // => headers as a Map
//    def body = msg.body              // => request body as a string
//    def status = msg.status          // => http status code of the response
//    def json = msg.json              // => any JSON included in response body, as a data structure of lists and maps
//    def xml = msg.xml                // => any XML included in response body, as a document tree structure
//    def data = msg.data              // => either JSON or XML in response body (whichever is specified by content-type header in response)

    def rpiNotification = msg.json
    
    if (rpiNotification.deviceId) {
    	Log("parse ${rpiNotification}")
    	def childId = getChildDeviceId(rpiNotification.deviceId)
        def childDevice = getChildDevices().find { d -> d.getDeviceNetworkId() == childId}
        if (childDevice) {
            def parseHandler = getDeviceHandlerToParseHandler(childDevice.getTypeName())
            parseHandler(childDevice, rpiNotification)
        }
    }
}
}

def setId() {
logEx {
    sendEvent (name: "id", value: device.getDeviceNetworkId())
}
}

def setOnline() {
logEx {
    Log("setOnline")
    sendEvent (name: "state", value: "online")
}
}

def setOffline() {
logEx {
    Log("setOffline")
    sendEvent (name: "state", value: "offline")
    
    getChildDevices().each { d -> d.initialize() }
}
}

def refreshChildren(rpiChildDevices) {
logEx {
    Log "Entered refreshChildren with ${rpiChildDevices}"
    def parentId = device.getDeviceNetworkId()
    def latestRpiDevices = rpiChildDevices.collect { rpiChildDevice -> getChildDeviceId(rpiChildDevice.key) }
    
    Log("latestRpiDevices ${latestRpiDevices}")
    def delete = getChildDevices().findAll { d -> !(latestRpiDevices.find { rpiDevId -> rpiDevId == d.getDeviceNetworkId() }) }
    delete.each { d -> deleteChildDevice(d.getDeviceNetworkId()) }
    
    rpiChildDevices.each { rpiChildDevice ->
        def rpiDeviceId = rpiChildDevice.key
        def rpiDeviceType = rpiChildDevice.value
        
        def childId = getChildDeviceId(rpiDeviceId)
        def childDeviceHandler = getRpiDeviceTypeToDeviceHandler(rpiDeviceType)
        
        
        Log "Processing childId: ${childId} handler: ${childDeviceHandler}"
        
        def childDevice = getChildDevice(childId)
        
        // ensure already existing device still has same handler
        if (childDevice && childDeviceHandler != childDevice.getTypeName()) {
            deleteChildDevice(childDevice.getDeviceNetworkId())
            childDevice = null
        }
        if (!childDevice) {
            Log "Creating RPI Device with dni: ${childId} and handler: ${childDeviceHandler}"
            childDevice = addChildDevice("abchez", childDeviceHandler, childId, hubId, [label: "RPI Sensor ${rpiDeviceId}"])
            childDevice.initialize()
        }
    }
}
return true
}

def getChildDeviceId (String childKey) {
    if (childKey.indexOf('/') >= 0) {
        return childKey
    }
    else {
    	return device.getDeviceNetworkId() + "/" + childKey
    }
}

def getChildDevice (String childKey) {
    def childId = getChildDeviceId(childKey)
    return getChildDevices()?.find { existing -> existing.getDeviceNetworkId() == childId }
}

def Log(String text) {
    parent.Log(text)
}

def logEx(closure) {
    try {
        closure()
        return true
    }
    catch (e) {
        parent.setError("${e}")
        parent.Log "EXCEPTION: ${e}"
        throw e
    }
}