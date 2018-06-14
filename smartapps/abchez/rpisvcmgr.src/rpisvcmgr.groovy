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
    page(name:"page1")
}

def page1() {
    dynamicPage(name:"page1", title:"Device Setup", install: true, uninstall: true) {
    	section {
        	input(name: "rpiIP", type: "text", title: "Raspberry PI IP address", required: true)
        	input(name: "rpiPort", type: "number", title: "Raspberry PI port", defaultValue: 8800, required: true)
            if (app.getInstallationState() == "COMPLETE") {
	            def c = getChildDevices().find { d -> true }
                if (!c) {
                    paragraph "Raspberry PI not installed or deleted."
                } else {
                    paragraph "Raspberry PI ${c.getDeviceNetworkId()} is ${c.currentValue("state")}"
                }
                def errorState = app.currentState("error")
                if (errorState && errorState.value != "") {
                	paragraph "${app.currentState("error").value}"
                }
            }
        }
    }
}

def installed() {
    Log("Installed with settings: ${settings}")
	initialize()
}

def uninstalled() {
}

def childUninstalled() {
}

def updated() {
	Log("Updated with settings: ${settings}")
    unschedule()
	unsubscribe()
    
	initialize()
}

def initialize() {

    state.retryCount = 0
    startSetup()
}

def startSetup () {
logEx {
    state.setupPending = true
    state.setupUUID = UUID.randomUUID().toString()
    runIn(20, setupTimeout, [data: [setupUUID: state.setupUUID]])

    subscribeRPI()
}
}

def setupTimeout(data) {
logEx {
	if (state.setupUUID != data.setupUUID)
    	return;
    
    state.setupPending = false

    Log("TIMEOUT setting up RPI Component Device")
    def errorState = app.currentState("error")
    if (!errorState || errorState.value == "") {
        setError("TIMEOUT setting up RPI Component Device")
    }

    if (state.retryCount == 2) {
        getChildDevices().each {
	    	d -> d.setOffline()
    		sendPushMessage("Raspberri PI ${d.getDeviceNetworkId()} is offline.");
    	}
    }
    
    //retry
    state.retryCount = state.retryCount + 1
    def timeToRetrySecs = (state.retryCount < 30 ? state.retryCount : 30) * 10
    Log("Retrying in ${timeToRetrySecs}...")
    runIn (timeToRetrySecs, startSetup)
}
}

def subscribeRPI () {
    Log("subscribeRPI")
    def hub = location.getHubs()[0]
    
    sendHubCommand(new physicalgraph.device.HubAction([
    	method: "PUT",
        path: "/subscribe",
        body: [ uuid: state.setupUUID, hubIP: hub.getLocalIP(), hubPort: hub.getLocalSrvPortTCP() ]
    ], "${rpiIP}:${rpiPort}", [callback: subscribeRPIcallback]))
}

def subscribeRPIcallback(physicalgraph.device.HubResponse hubResponse) {
logEx {
    if (!state.setupPending)
    	return;

    if (hubResponse?.json?.uuid == "${state.setupUUID}") {
        Log("subscribeRPIcallback ${state.setupUUID}")
        def piDevices = hubResponse?.json?.devices;
        if (piDevices?.size() > 0) {
            return createOrUpdateComponentdevice(hubResponse.hubId, hubResponse.mac, piDevices)
        }
    }
}
}

def createOrUpdateComponentdevice(hubId, mac, piDevices) {
    Log("createOrUpdateComponentdevice")
    def deviceLabel = "RPI ${rpiIP}"

    def delete = getChildDevices().findAll { d -> d.getDeviceNetworkId() != mac || d.getLabel() != deviceLabel }
    delete.each { d -> deleteChildDevice(d.getDeviceNetworkId()) }

    def piComponentDevice = getChildDevices()?.find { d -> d.getDeviceNetworkId() == mac && d.getLabel() == deviceLabel }
    if (!piComponentDevice) {
        Log("Creating RPI Component Device with dni: ${mac}")
        piComponentDevice = addChildDevice("abchez", "RPI Component Device", mac, hubId, [label: deviceLabel])
        piComponentDevice.setId()
    }
    app.updateLabel("Raspberry PI ${rpiIP} ${mac}")
    
    if (piComponentDevice.refreshChildren(piDevices)) {
        setSetupCompleted()
    }
}

def setSetupCompleted() {
    getChildDevices().each { 
    	d -> d.setOnline()
        if (state.retryCount > 2) {
    		sendPushMessage("Raspberri PI ${d.getDeviceNetworkId()} is back online.");
    	}
    }

    unschedule('setupTimeout')
    state.setupPending = false
    state.lastSetupCompleted = new Date ()
    state.retryCount = 0
    Log("SUCCESS setting up RPI Component Device")
    
    runIn (60 * 5, startSetup)
    
    def errorState = app.currentState("error")
    if (errorState && errorState.value != "") {
        setError("")
    }
}

def Log(String text) {
    //sendEvent(name: "log", value: text)
    log.debug text
}

def setError(String text) {
    state.setErrorTime = new Date ()
    sendEvent(name: "error", value: text)
}

def logEx(closure) {
    try {
        closure()
    }
    catch (e) {
        setError("${e}")
        Log("EXCEPTION error: ${e}")
        throw e
    }
}