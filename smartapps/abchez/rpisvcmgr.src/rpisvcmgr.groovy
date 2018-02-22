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
    iconX3Url: "https://www.unifiedremote.com/content/logos/RaspberryPi.png",
    singleInstance: true)



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

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    
    pingPI()
}


def pingPI() {
    
    sendHubCommand(new physicalgraph.device.HubAction([
    	method: "GET",
        path: "/ping",
        body: [value : "hello"]
    ], "${rpiIP}:${rpiPort}", [callback: pingPIcallback]))
}

def pingPIcallback(physicalgraph.device.HubResponse hubResponse) {
	log.debug hubResponse.json
	if (hubResponse?.json?.value == "hello") {
    	def hub = location.getHubs().find { hub -> hub.id == hubResponse.hubId }
        if (hub != null) {
            subscribePI(hub)
        }
    }
}

def subscribePI (physicalgraph.app.HubWrapper hub) {
	log.debug "subscribePI"
    sendHubCommand(new physicalgraph.device.HubAction([
    	method: "PUT",
        path: "/subscribe",
        body: [ hubIP: hub.getLocalIP(), hubPort: hub.getLocalSrvPortTCP() ]
    ], "${rpiIP}:${rpiPort}", [callback: subscribePIcallback]))
}

def subscribePIcallback(physicalgraph.device.HubResponse hubResponse) {
	log.debug "subscribePIcallback" 
    def piDevices = hubResponse?.json?.devices;
    if (piDevices?.size() > 0) {
    	createOrUpdateComponentdevice(hubResponse.hubId, hubResponse.mac, piDevices)
    }
}

def createOrUpdateComponentdevice(hubId, mac, piDevices) {

    def delete = getChildDevices().findAll { d -> d.getDeviceNetworkId() != mac }
    delete.each { d -> deleteChildDevice(d.getDeviceNetworkId()) }

    def piComponentDevice = getChildDevices()?.find { d -> d.getDeviceNetworkId() == mac }
    if (!piComponentDevice) {
        log.debug "Creating RPI Component Device with dni: ${mac}"
        piComponentDevice = addChildDevice("abchez", "RPI Component Device", mac, hubId)
    }
    log.debug piComponentDevice.refreshChildren(piDevices)
}

def Log(text) {
    log.debug text
}
