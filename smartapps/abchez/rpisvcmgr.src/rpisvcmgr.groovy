/**
 *  RpiSvcMgr
 *
 *  Copyright 2018 abchez
 *
 */
definition(
    name: "RpiSvcMgr",
    namespace: "abchez",
    author: "abchez",
    description: "Raspberry PI svc mgr",
    category: "My Apps",
    iconUrl: "https://www.unifiedremote.com/content/logos/RaspberryPi.png",
    iconX2Url: "https://www.unifiedremote.com/content/logos/RaspberryPi.png",
    iconX3Url: "https://www.unifiedremote.com/content/logos/RaspberryPi.png")



preferences {
	page(name:"page1", title:"Device Setup", install: true, uninstall: true) {
    	section {
        	input(name: "rpiIP", type: "text", title: "Raspberry PI IP address", required: true)
        	input(name: "rpiPort", type: "number", title: "Raspberry IP port", defaultValue: 8800, required: true)
        }
    }    
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def uninstalled() {
}

def updated() {
	log.debug "Updated with settings: ${settings}"

    unschedule()
	unsubscribe()
	initialize()
}

def initialize() {
    state.setupPending = false
    state.retryCount = 0
    startSetup()
}

def setupTimeout() {
	if (!state.setupPending)
    	return;
        
    log.debug "TIMEOUT setting up RPI Component Device"
        
    setSetupError()
 }
 
 def startSetup () {
 	if (state.setupPending)
    	return;
        
    state.setupPending = true
    runIn(10, setupTimeout)

    subscribeRPI()
 }
 
 def setSetupError() {
    if (!state.setupPending)
    	return;
        
    state.setupPending = false
    log.debug "ERROR setting up RPI Component Device"
    
    getChildDevices().each { 
    	d -> d.setOffline()
        if (state.retryCount == 0) {
    		sendPushMessage("Raspberri PI ${d.getDeviceNetworkId()} is offline.");
    	}
    }
    
    
    //retry
    state.retryCount = state.retryCount + 1
    def timeToRetrySecs = (state.retryCount < 30 ? state.retryCount : 30) * 10
    log.debug "Retrying in ${timeToRetrySecs}..."
    runIn (timeToRetrySecs, startSetup)
}

def setSetupCompleted() {
    if (!state.setupPending)
    	return;

    getChildDevices().each { 
    	d -> d.setOnline()
        if (state.retryCount > 0) {
    		sendPushMessage("Raspberri PI ${d.getDeviceNetworkId()} is back online.");
    	}
    }

    unschedule('setupTimeout')
    state.setupPending = false
    state.retryCount = 0
    log.debug "SUCCESS setting up RPI Component Device"
    
    runIn (60 * 5, startSetup)
    
}

def pingRPI() {
    if (!state.setupPending)
    	return;
        
	log.debug "pingRPI"
    state.setupUUID = UUID.randomUUID().toString()
    sendHubCommand(new physicalgraph.device.HubAction([
    	method: "GET",
        path: "/ping",
        body: [uuid : "${state.setupUUID}"]
    ], "${rpiIP}:${rpiPort}", [callback: pingRPIcallback]))
}

def pingRPIcallback(physicalgraph.device.HubResponse hubResponse) {
    if (!state.setupPending)
    	return;
        
	log.debug "pingRPIcallback ${hubResponse} ${hubResponse.json}"
	if (hubResponse?.json?.uuid == "${state.setupUUID}") {
    	def hub = location.getHubs().find { hub -> hub.id == hubResponse.hubId }
        if (hub != null) {
            subscribeRPI(hub)
        }
    }
}

def subscribeRPI () {
    if (!state.setupPending)
    	return;

    log.debug "subscribeRPI"
    def hub = location.getHubs()[0]
    
    state.setupUUID = UUID.randomUUID().toString()
    sendHubCommand(new physicalgraph.device.HubAction([
    	method: "PUT",
        path: "/subscribe",
        body: [ uuid: state.setupUUID, hubIP: hub.getLocalIP(), hubPort: hub.getLocalSrvPortTCP() ]
    ], "${rpiIP}:${rpiPort}", [callback: subscribeRPIcallback]))
}

def subscribeRPIcallback(physicalgraph.device.HubResponse hubResponse) {
    if (!state.setupPending)
    	return;

	log.debug "subscribeRPIcallback ${hubResponse} ${hubResponse.json}"
    if (hubResponse?.json?.uuid == "${state.setupUUID}") {
        def piDevices = hubResponse?.json?.devices;
        if (piDevices?.size() > 0) {
            return createOrUpdateComponentdevice(hubResponse.hubId, hubResponse.mac, piDevices)
        }
    }
}

def createOrUpdateComponentdevice(hubId, mac, piDevices) {
    if (!state.setupPending)
    	return;

	log.debug "createOrUpdateComponentdevice" 
    def delete = getChildDevices().findAll { d -> d.getDeviceNetworkId() != mac }
    delete.each { d -> deleteChildDevice(d.getDeviceNetworkId()) }

    def piComponentDevice = getChildDevices()?.find { d -> d.getDeviceNetworkId() == mac }
    if (!piComponentDevice) {
        log.debug "Creating RPI Component Device with dni: ${mac}"
        piComponentDevice = addChildDevice("abchez", "RPI Component Device", mac, hubId)
    }
    piComponentDevice.refreshChildren(piDevices)
    setSetupCompleted()
}


def Log(String text) {
    log.debug text
}
