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
    description: "HTTP sensor server svc mgr",
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
        	input(name: "rpiIP", type: "text", title: "IP address", required: true)
        	input(name: "rpiPort", type: "number", title: "Port", defaultValue: 8800, required: true)
        	input(name: "rpiLabel", type: "text", title: "Name", required: false)
            if (app.getInstallationState() == "COMPLETE") {
	            def c = getChildDevices().find { d -> true }
                if (!c) {
                    paragraph "HTTP sensor server device not installed or deleted."
                } else {
                    paragraph "HTTP sensor server device ${c.getDeviceNetworkId()} is ${c.currentValue("state")}"
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
    if (!state.setupPending) {
        unschedule()
        unsubscribe()

        initialize()
    }
}

def initialize() {

    def appLabel = settings.rpiLabel;
    if (appLabel == null || appLabel == "" || appLabel == "RpiSvcMgr") {
        appLabel = "HTTP sensors ${rpiIP}";
    }
    
    app.updateLabel(appLabel)
    state.retryCount = 0
    startSetup()
}

def startSetup () {
logEx {
    Log("startSetup")
    state.setupPending = true
    state.setupUUID = Long.toHexString((Math.random() * 0x100000).round())
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
    		sendPushMessage("${app.getLabel()} is offline.");
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
    Log("createOrUpdateComponentdevice ${rpiLabel}")
    
    def appLabel = settings.rpiLabel;
    if (appLabel == null || appLabel == "" || appLabel == "RpiSvcMgr") {
        appLabel = "HTTP sensors ${rpiIP} ${mac}";
    }
    app.updateLabel(appLabel)
    
    def delete = getChildDevices().findAll { d -> d.getDeviceNetworkId() != mac }
    delete.each { d -> deleteChildDevice(d.getDeviceNetworkId()) }

    def piComponentDevice = getChildDevices()?.find { d -> d.getDeviceNetworkId() == mac }
    if (!piComponentDevice) {
        Log("Creating RPI Component Device with dni: ${mac}")
        def deviceLabel = "HTTP sensors ${rpiIP}"
        piComponentDevice = addChildDevice("abchez", "RPI Component Device", mac, hubId, [label: deviceLabel])
        piComponentDevice.setId()
    }
    
    if (piComponentDevice.refreshChildren(piDevices)) {
        setSetupCompleted()
    }
}

def setSetupCompleted() {
    getChildDevices().each { 
    	d -> d.setOnline()
        if (state.retryCount > 2) {
    		sendPushMessage("${app.getLabel()} is back online.");
    	}
    }

    state.setupPending = false
    state.lastSetupCompleted = new Date ()
    state.retryCount = 0
    Log("SUCCESS setting up RPI Component Device")
    
    def errorState = app.currentState("error")
    if (errorState && errorState.value != "") {
        setError("")
    }

    unschedule('startSetup')
    runIn (60 * 15, startSetup)
    unschedule('setupTimeout')
}

def Log(String text) {
    sendEvent(name: "log", value: text)
    //log.debug text
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