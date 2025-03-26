/**
 *  Hubitat - CA Driver - XPort - Dimmer Module - by VH
 *
 *  Copyright 2024 VH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *        1.0 09/8/2024  - V.BETA 1 
 *        1.1 18/3/2025  - Fix ON Value to 255 when ON Dimmer 100%.
 *        1.2 26/3/2025  - Fixed feedback and added "contains" macaddress for parse IF  filter response for module listening
 * 			    Improved connection handling and online status
 */
metadata {
    definition (name: "Controlart - Xport - Xbus DIMMER Module v2", namespace: "VH", author: "VH", vid: "generic-contact") { 
        capability "Configuration"
        capability "Initialize" 
        capability "Refresh"
        capability "Switch"  
        capability "SwitchLevel"
        capability "ChangeLevel"       
    }
}

import groovy.json.JsonSlurper
import groovy.transform.Field

command "keepalive"
command "getmac"
command "getstatus"
command "reconnect"
command "defaults"

@Field static final String DRIVER = "by TRATO"
@Field static final String USER_GUIDE = "https://github.com/hhorigian/hubitat_MolSmart_Relays/tree/main/TCP"

String fmtHelpInfo(String str) {
    String prefLink = "<a href='${USER_GUIDE}' target='_blank'>${str}<br><div style='font-size: 70%;'>${DRIVER}</div></a>"
    return "<div style='font-size: 160%; font-style: bold; padding: 2px 0px; text-align: center;'>${prefLink}</div>"
}

preferences {
    input "device_IP_address", "text", title: "IP Address of Xport", required: true
    input "device_port", "number", title: "IP Port of Xport", required: true, defaultValue: 4998
    input "module_mac", "text", title: "Mac Address of Module" 
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    input "minLevel",
        "number",
        title: "Minimum level",
        description: "Minimum brightness level (%). 0% on the dimmer level is mapped to this.",
        required: true,
        multiple: false,
        defaultValue: 0
    
    if (minLevel < 0) {
        minLevel = 0
    } else if (minLevel > 99) {
        minLevel = 99
    }
  
    input 'logInfo', 'bool', title: 'Show Info Logs?', required: false, defaultValue: true
    input 'logWarn', 'bool', title: 'Show Warning Logs?', required: false, defaultValue: true
    input 'logDebug', 'bool', title: 'Show Debug Logs?', description: 'Only leave on when required', required: false, defaultValue: true
    input 'logTrace', 'bool', title: 'Show Detailed Logs?', description: 'Only leave on when required', required: false, defaultValue: true

    //help guide
    input name: "UserGuide", type: "hidden", title: fmtHelpInfo("Manual do Driver") 
    
    attribute "boardstatus", "string"
}   

@Field static final Integer CONNECTION_CHECK_INTERVAL = 150 // seconds
@Field static final Integer RECONNECT_DELAY = 10 // seconds
@Field static final Integer MAX_RECONNECT_ATTEMPTS = 3

@Field static String partialMessage = ''
@Field static Integer checkInterval = 150
@Field static Integer connectionTimeout = 30000 // 30 seconds

def installed() {
    logTrace('installed()')
    state.clear()
    state.childscreated = "0"
    state.boardstatus = "offline"
    runIn(1800, logsOff)
}

def uninstalled() {
    logTrace('uninstalled()')
    unschedule()
    closeConnection()
}

def updated() {
    logTrace('updated()')
    initialize()
}


def defaults() {
    state.inputcount = 3
    state.outputcount = 3  
    state.dim1 = "0"
    state.dim2 = "0"
    state.dim3 = "0" 
    log.info "Run Defaults"
}

def initialize() {
    unschedule()
    logTrace('Run Initialize()')
    
    partialMessage = ''
    state.connectionAttempts = 0
    state.lastMessageReceivedAt = 0
    
    // Initialize state variables
    state.inputcount = 3
    state.outputcount = 3  
    state.dim1 = "0"
    state.dim2 = "0"
    state.dim3 = "0" 
    
    if (!device_IP_address) {
        logError 'IP address not configured'
        updateStatus("offline")
        return
    }

    if (!device_port) {
        logError 'Port not configured'
        updateStatus("offline")
        return
    }
    
    establishConnection()
    
    // Create child devices if needed
    if (state.childscreated == 0) {
        createchilds()
    }
    
    // Get initial status
    runIn(2, "getstatus")
    runIn(10, "refresh")
}

def establishConnection() {
    try {
        logDebug("Attempting connection to ${device_IP_address}:${device_port}")
        // Corrected connect() call without timeout parameter
        interfaces.rawSocket.connect(device_IP_address, device_port.toInteger())
        state.lastMessageReceivedAt = now()
        state.connectionAttempts = 0
        updateStatus("online")
        runIn(CONNECTION_CHECK_INTERVAL, "connectionCheck")
        logInfo("Connection established successfully")
    } catch (e) {
        handleConnectionError(e)
    }
}


def handleConnectionError(Exception e) {
    state.connectionAttempts = (state.connectionAttempts ?: 0) + 1
    logError("Connection attempt ${state.connectionAttempts} failed: ${e.message}")
    
    if (state.connectionAttempts < MAX_RECONNECT_ATTEMPTS) {
        def retryDelay = RECONNECT_DELAY * state.connectionAttempts
        logWarn("Retrying connection in ${retryDelay} seconds...")
        runIn(retryDelay, "reconnect")
    } else {
        logError("Max reconnection attempts (${MAX_RECONNECT_ATTEMPTS}) reached")
        updateStatus("offline")
    }
}

def reconnect() {
    logDebug("Attempting reconnection")
    closeConnection()
    runIn(1, "establishConnection") // Small delay before reconnecting
}

def connectionCheck() {
    def now = now()
    def timeSinceLastMessage = now - (state.lastMessageReceivedAt ?: 0)
    
    if (timeSinceLastMessage > (CONNECTION_CHECK_INTERVAL * 1000)) {
        logWarn("No communication for ${timeSinceLastMessage/1000} seconds")
        if (isSocketConnected()) {
            // Socket is connected but no messages - send ping
            logDebug("Connection alive but no messages - sending ping")
            sendCommand("ping")
            runIn(CONNECTION_CHECK_INTERVAL, "connectionCheck")
        } else {
            // Socket is disconnected
            logWarn("Socket disconnected - attempting reconnect")
            updateStatus("offline")
            reconnect()
        }
    } else {
        // Connection is healthy
        updateStatus("online")
        runIn(CONNECTION_CHECK_INTERVAL, "connectionCheck")
    }
}

def isSocketConnected() {
    try {
        // Alternative connection check since we can't use sendMessage("")
        // Check if we've received any data recently
        return (now() - (state.lastMessageReceivedAt ?: 0)) < (CONNECTION_CHECK_INTERVAL * 1000 * 2)
    } catch (e) {
        return false
    }
}

def closeConnection() {
    try {
        interfaces.rawSocket.close()
        logDebug("Connection closed")
        state.lastMessageReceivedAt = 0
    } catch (e) {
        logDebug("Error closing connection: ${e.message}")
    }
}

def updateStatus(newStatus) {
    if (state.boardstatus != newStatus) {
        state.boardstatus = newStatus
        sendEvent(name: "boardstatus", value: newStatus, isStateChange: true)
        logInfo("Device status changed to: ${newStatus}")
        
        if (newStatus == "online") {
            // When coming online, immediately refresh status
            runIn(1, "refresh")
        }
    }
}



def createchilds() {
    if (state.childscreated == 0) {
        String thisId = device.id
        state.netids = "${thisId}-Switch-"
        log.info "Creating Childs. Info thisid = " + thisId
        
        for(int i = 1; i <= state.outputcount; i++) {        
            def cd = getChildDevice("${thisId}-Switch-${i}")
            if (!cd) {
                cd = addChildDevice("hubitat", "Generic Component Dimmer", "${thisId}-Switch-${i}", 
                    [name: "${device.displayName} Switch-${i}", isComponent: true])
                log.info "added switch # ${i} from ${state.outputcount}"            
            }
        }  
        state.childscreated = 1
    }
}

def getstatus() {
    pauseExecution(100)
    if (state.newmacdec) {
        def msg = "mdcmd_getmd,${state.newmacdec}"
        logTrace('Sent getstatus()')
        sendCommand(msg)
    } else {
        getmac()
    }
}

def getmac() {
    if (module_mac) {
        def macaddr1 = hubitat.helper.HexUtils.hexStringToInt(module_mac[0..1])
        def macaddr2 = hubitat.helper.HexUtils.hexStringToInt(module_mac[3..4])
        def macaddr3 = hubitat.helper.HexUtils.hexStringToInt(module_mac[6..7]) 
        logTrace('Run getmac()')
        def newmac = "${macaddr1},${macaddr2},${macaddr3}"
        state.newmacdec = newmac
        state.macaddress = module_mac
        log.info "Mac HEX = ${module_mac}"
        log.info "Mac Decimal = ${newmac} addr1 = ${macaddr1} addr2 = ${macaddr2} addr3 = ${macaddr3}"
    } else {
        logWarn("MAC address not configured")
    }
}

def refresh() {
    if (state.newmacdec) {
        def msg = "mdcmd_getmd,${state.newmacdec}"
        logTrace('Sent refresh()')   
        sendCommand(msg)
    } else {
        getmac()
    }
}

def parse(msg) {
    state.lastMessageReceived = new Date(now()).toString()
    state.lastMessageReceivedAt = now()

    // Only update status if we were previously offline
    if (state.boardstatus != "online") {
        updateStatus("online")
    }


    def newmsg = hubitat.helper.HexUtils.hexStringToByteArray(msg)
    def newmsg2 = new String(newmsg)    
    state.lastmessage = newmsg2
    
    log.info "qtde chars = ${newmsg2.length()}"
    log.info "lastmessage = ${newmsg2}"

    // Firmware version
    if (newmsg2.length() < 7) {
        state.firmware = newmsg2
        log.info "FW = ${newmsg2}"
    }

    // MAC address
    if (newmsg2.contains("macaddr_") && (newmsg2.length() == 21)) {
        def mac = newmsg2
        def newmac = (mac.substring(10)).replaceAll(",","0x").replaceAll("-",",0x").replaceAll("\\s","")
        log.info "Got macaddress = ${newmac}" 
        state.macaddress = newmac
        updateStatus("online")
    }
    
    // Status update
    if (newmsg2.contains("setmd") && (newmsg2.length() < 36) && (newmsg2.contains(module_mac))) {
        log.info "GETSTATUS AUTOMATIC FROM CHANGE"
        log.info "Message for = " + newmsg2
        log.info "This board has mac  = " + module_mac
        updateStatus("online")
        
        String thisId = device.id
        log.info "thisID = ${thisId}"
        
        def cddim1 = getChildDevice("${thisId}-Switch-1")
        def cddim2 = getChildDevice("${thisId}-Switch-2")
        def cddim3 = getChildDevice("${thisId}-Switch-3")
        
        String[] str = newmsg2.split(',')
        def dim1 = str[5] as Integer  
        def dim2 = str[6] as Integer
        def dim3 = str[7] as Integer   

        def olddim1 = state.dim1  
        def olddim2 = state.dim2  
        def olddim3 = state.dim3  
        
        state.dim1 = dim1
        state.dim2 = dim2
        state.dim3 = dim3
        
        updateChildDeviceStatus(cddim1, olddim1, dim1)
        updateChildDeviceStatus(cddim2, olddim2, dim2)
        updateChildDeviceStatus(cddim3, olddim3, dim3)
    }
}

def updateChildDeviceStatus(cd, oldValue, newValue) {
    if (oldValue != newValue) {
        def valorsetlevel = valueToLevel(newValue)
        def switchState = (valorsetlevel > 0) ? "on" : "off"
        
        getChildDevice(cd.deviceNetworkId).parse([
            [name: "switch", value: switchState, 
             descriptionText: "${cd.displayName} was turned ${switchState} via ParseSetLevel", 
             isStateChange: true],
            [name: "level", value: valorsetlevel, 
             descriptionText: "${cd.displayName} was dimmered NEW VIA Parse"]
        ])
    }
}

// Commands
def on() {
    logDebug("Master Power ON()")
    if (state.macaddress) {
        def msg = "mdcmd_setallonmd,${state.macaddress}\r\n"
        sendCommand(msg)
        pauseExecution(300)
        getstatus()
    }
}

def off() {
    logDebug("Master Power OFF()")
    if (state.macaddress) {
        def msg = "mdcmd_setmasteroffmd,${state.macaddress}\r\n"
        sendCommand(msg)
        pauseExecution(300)
        getstatus()
    }
}

// Modified sendCommand with simplified connection checking
private sendCommand(s) {
    try {
        logDebug("sendingCommand ${s}")
        interfaces.rawSocket.sendMessage(s)
        state.lastCommandSentAt = now()
        return true
    } catch (e) {
        logError("Error sending command: ${e.message}")
        updateStatus("offline")
        runIn(RECONNECT_DELAY, "reconnect")
        return false
    }
}


// Component Child methods
void componentRefresh(cd) {
    if (logEnable) log.info "received refresh request from ${cd.displayName}"
    refresh()
}

def componentOn(cd) {
    if (logEnable) log.info "received on request from ${cd.displayName}"
    getChildDevice(cd.deviceNetworkId).parse([[name:"switch", value:"on", descriptionText:"${cd.displayName} was turned on"]])       
    on(cd)  
    pauseExecution(100)
}

void componentOff(cd) {
    if (logEnable) log.info "received off request from ${cd.displayName}"
    getChildDevice(cd.deviceNetworkId).parse([[name:"switch", value:"off", descriptionText:"${cd.displayName} was turned off"]])    
    off(cd)
    pauseExecution(100)
}

void componentSetLevel(cd, level) {
    if (logEnable) log.info "received set level dimmer from ${cd.displayName}"
    def valueaux = level as Integer
    def level2 = Math.max(Math.min(valueaux, 99), 0)
    
    if (level2 > 0) {
        getChildDevice(cd.deviceNetworkId).parse([
            name: "switch", 
            value: "on", 
            descriptionText: "${cd.displayName} was turned on via ComponentSetLevel > 0", 
            isStateChange: true
        ])    
    } else {
        getChildDevice(cd.deviceNetworkId).parse([
            name: "switch", 
            value: "off", 
            descriptionText: "${cd.displayName} was turned off via ComponentSetLevel < 0", 
            isStateChange: true
        ])    
    }
    
    SetLevel(cd, level)   
    getChildDevice(cd.deviceNetworkId).parse([
        name: "level", 
        value: level2, 
        descriptionText: "${cd.displayName} was dimmered via Interface"
    ]) 
}

void SetLevel(cd, level) {
    def value2 = levelToValue(level)
    def relay = extractRelayNumber(cd.deviceNetworkId)
    
    if (relay != null && state.newmacdec) {
        def comando = "mdcmd_sendmd,${state.newmacdec},${relay},${value2}\r\n"
        interfaces.rawSocket.sendMessage(comando)   
        logDebug("Foi Alterado o Dimmer ${relay} via TCP ${comando}")
        state.update = 1
    }
}

void on(cd) {
    def relay = extractRelayNumber(cd.deviceNetworkId)
    
    if (relay != null && state.newmacdec) {
        def comando = "mdcmd_sendmd,${state.newmacdec},${relay},255\r\n"
        interfaces.rawSocket.sendMessage(comando)
        log.info "Foi Ligado o Relay ${relay} via TCP ${comando}"
        state.update = 1
    }
}

void off(cd) {
    def relay = extractRelayNumber(cd.deviceNetworkId)
    
    if (relay != null && state.newmacdec) {
        def comando = "mdcmd_sendmd,${state.newmacdec},${relay},0\r\n"
        interfaces.rawSocket.sendMessage(comando)
        log.info "Foi Desligado o Relay ${relay} via TCP ${comando}" 
        state.update = 1
    }
}

Integer extractRelayNumber(String deviceNetworkId) {
    try {
        def substr1 = deviceNetworkId.indexOf("-", deviceNetworkId.indexOf("-") + 1)
        def result01 = deviceNetworkId.length() - substr1 
        
        if (result01 > 2) {
            def substr2a = substr1 + 1
            def substr2b = substr1 + 2
            return deviceNetworkId[substr2a..substr2b].toInteger() - 1
        } else {
            return deviceNetworkId[substr1+1].toInteger() - 1
        }
    } catch (e) {
        logError("Error extracting relay number: ${e.message}")
        return null
    }
}

// Level conversion methods
def levelToValue(BigDecimal level) {
    return levelToValue(level.toInteger())
}

def levelToValue(Integer level) {
    Integer minValue = Math.round(settings.minLevel*2.55)
    return rescale(level, 0, 100, minValue, 255)
}

def valueToLevel(Integer value) {
    Integer minValue = Math.round(settings.minLevel*2.55)
    if (value < minValue) {
        return 0
    } else {
        return rescale(value, minValue, 255, 0, 100)
    }
}

// Helper methods
def rescale(value, fromLo, fromHi, toLo, toHi) {
    return Math.round((((value-fromLo)*(toHi-toLo))/(fromHi-fromLo)+toLo))
}

// Logging methods
def logsOff() {
    log.warn 'logging disabled...'
    device.updateSetting('logInfo', [value:'false', type:'bool'])
    device.updateSetting('logWarn', [value:'false', type:'bool'])
    device.updateSetting('logDebug', [value:'false', type:'bool'])
    device.updateSetting('logTrace', [value:'false', type:'bool'])
}

void logDebug(String msg) {
    if ((Boolean)settings.logDebug != false) {
        log.debug "${device.displayName}: ${msg}"
    }
}

void logInfo(String msg) {
    if ((Boolean)settings.logInfo != false) {
        log.info "${device.displayName}: ${msg}"
    }
}

void logTrace(String msg) {
    if ((Boolean)settings.logTrace != false) {
        log.trace "${device.displayName}: ${msg}"
    }
}

void logWarn(String msg, boolean force = false) {
    if (force || (Boolean)settings.logWarn != false) {
        log.warn "${device.displayName}: ${msg}"
    }
}

void logError(String msg) {
    log.error "${device.displayName}: ${msg}"
}
