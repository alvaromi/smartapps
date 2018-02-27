/**
 *  RPI Motion Sensor Device
 *
 *  Copyright 2018 Alvaro Miranda 
 *
 */
metadata {
	definition (name: "RPI Motion Sensor Device", namespace: "abchez", author: "Alvaro Miranda") {
		capability "Motion Sensor"
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

def setActive(data) {
	Log("setActive ${data}")
	return sendEvent(name: "motion", value: "active", data : data)
}

def setInactive(data) {
	Log("setInactive ${data}")
    return sendEvent(name: "motion", value: "inactive", data : data)
}

def initialize() {
    setInactive()
}

def Log(String text) {
    parent.Log(text)
}