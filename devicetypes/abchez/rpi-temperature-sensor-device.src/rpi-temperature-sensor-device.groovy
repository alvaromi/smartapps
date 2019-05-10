/**
 *  RPI Temperature Sensor Device
 *
 *  Copyright 2018 Alvaro Miranda 
 *
 */
metadata {
	definition (name: "RPI Temperature Sensor Device", namespace: "abchez", author: "Alvaro Miranda") {
		capability "Temperature Measurement"
        capability "Thermostat" // this is so Alexa can identify the device 
	}

	tiles(scale: 2) {
        valueTile("temperature", "device.temperature", height: 6, width: 6) {
            state "default", label:'${currentValue}°', unit:"F", icon:"st.alarm.temperature.normal",
                backgroundColors:[
                    [value: null, color: "#a0a0a0"],
                    [value: 31, color: "#153591"],
                    [value: 44, color: "#1e9cbb"],
                    [value: 59, color: "#90d2a7"],
                    [value: 74, color: "#44b621"],
                    [value: 84, color: "#f1d801"],
                    [value: 95, color: "#d04e00"],
                    [value: 96, color: "#bc2323"]
                ]
        }
		main "temperature"
		details "temperature"
    }
}

// parse events into attributes
def parse(String description) {
	Log("parse ${description}")
}

def getCurrentState() {
    return device.currentValue("temperature")
}

def setCurrentState(value, data) {
logEx {
    Log("set ${device.getDeviceNetworkId()} ${value} ${data}")
	sendEvent(name: "temperature", value: value, unit: "F", data : data)
    if (data) {
        state.session =  data.session
        state.eventTime = data.eventTime
    } else {
        state.session = null
        state.eventTime = null
    }
}
}

def initialize() {
    sendEvent(name: "supportedThermostatModes", value: ["off"], displayed: false)
    sendEvent(name: "thermostatMode", value: "off", displayed: false)
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