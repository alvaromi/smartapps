/**
 *  RPI Motion Sensor Device
 *
 *  Copyright 2018 Alvaro Miranda 
 *
 */
metadata {
	definition (name: "RPI Motion Sensor Device", namespace: "abchez", author: "Alvaro Miranda") {
		capability "Motion Sensor"
        attribute "session", "string"
        attribute "eventTime", "string"
	}


	simulator {
		status "inactive": "motion:inactive"
		status "active": "motion:active"
	}

	tiles(scale: 2) {
    	
		multiAttributeTile(name:"motion", type: "generic", width: 6, height: 4){
			tileAttribute ("device.motion", key: "PRIMARY_CONTROL") {
				attributeState "inactive", label:'no motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff"
				attributeState "active", label:'motion', icon:"st.motion.motion.active", backgroundColor:"#00a0dc"
			}
		}
		main "motion"
		details "motion"
	}
}

// parse events into attributes
def parse(String description) {
	Log("parse ${description}")
}

def getCurrentState() {
    return device.currentValue("motion")
}

def setCurrentState(value, data) {
	Log("set ${device.getDeviceNetworkId()} ${value} ${data}")
	sendEvent(name: "motion", value: value, data : data)
    if (data) {
        state.session = data.session
        state.eventTime = data.eventTime
    }
}

def initialize() {
}

def Log(String text) {
    parent.Log(text)
}
