/**
 *  RPI Contact Sensor Device
 *
 *  Copyright 2018 Alvaro Miranda 
 *
 */
metadata {
	definition (name: "RPI Contact Sensor Device", namespace: "abchez", author: "Alvaro Miranda") {
		capability "Contact Sensor"
	}


	simulator {
		status "closed": "contact:closed"
		status "open": "contact:open"
	}

	tiles(scale: 2) {
    	
		multiAttributeTile(name:"contact", type: "generic", width: 6, height: 4){
			tileAttribute ("device.contact", key: "PRIMARY_CONTROL") {
				attributeState "closed", label:'closed', icon:"st.contact.contact.closed", backgroundColor:"#ffffff"
				attributeState "open", label:'open', icon:"st.contact.contact.open", backgroundColor:"#00a0dc"
			}
		}
		main "contact"
		details "contact"
	}
}

// parse events into attributes
def parse(String description) {
	Log("parse ${description}")
}

def getCurrentState() {
    return device.currentValue("contact")
}

def setCurrentState(id, value, data) {
	Log("set ${id} ${value} ${data}")
	sendEvent(name: "contact", value: value, data : data)
    state.session =  data.session
    state.eventTime = data.eventTime
}

def initialize() {
    setContactState("closed")
}

def Log(String text) {
    parent.Log(text)
}
