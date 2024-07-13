/**
 *  ControlArt Xport Driver - IR para TV. Usando Xport
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
 *
 *            --- Driver para XPort - IR - para TV  e Som --- Usando Controles criados na Xport 
 *           v.1  12/07/2024 - BETA. 
 *
 */
metadata {
  definition (name: "ControlArt - Xport - IR para TV e SOM", namespace: "TRATO", author: "VH", vid: "generic-contact") {
    capability "Switch"  
    capability "Actuator"
    capability "TV"  
    capability "SamsungTV"
    capability "PushableButton"
	capability "Variable"  
    capability "Configuration"
    capability "Initialize"       
  
    attribute "boardstatus", "string"   
	attribute "channel", "number"
	attribute "volume", "number"
	attribute "movieMode", "string"
	attribute "power", "string"
	attribute "sound", "string"
	attribute "picture", "string"  
   
        command "mute"      
        command "source"
        command "back"   
        command "menu"
        command "hdmi1"
   	    command "hdmi2"  
        command "up"
    	command "down"
    	command "right"
    	command "left"
   	    command "confirm"
        command "exit"   
        command "home"
        command "channelUp"
        command "channelDown"
        command "volumeUp"
   	    command "volumeDown"
    	command "num0" 
        command "num1"
    	command "num2"
    	command "num3"
    	command "num4"
    	command "num5"
    	command "num6"
    	command "num7"
    	command "num8"
    	command "num9"
        command "btnextra1" 
    	command "btnextra2"       
    	command "btnextra3"    
        command "appnetflix" 
        command "appamazon"
        command "appyoutube"  
          
  }
      
}

import groovy.json.JsonSlurper
import groovy.transform.Field

command "keepalive"
command "reconnect"
command "refresh"

    import groovy.transform.Field
    @Field static final String DRIVER = "by TRATO"
    @Field static final String USER_GUIDE = "https://github.com/hhorigian/hubitat_MolSmart_GW3_IR/tree/main/TV"


    String fmtHelpInfo(String str) {
    String prefLink = "<a href='${USER_GUIDE}' target='_blank'>${str}<br><div style='font-size: 70%;'>${DRIVER}</div></a>"
    return "<div style='font-size: 160%; font-style: bold; padding: 2px 0px; text-align: center;'>${prefLink}</div>"
    }


  preferences {
    input "device_IP_address", "text", title: "IP Address of Xport", required: true
    input "device_port", "number", title: "IP Port of Xport", required: true, defaultValue: 4998
//  input "module_mac", "text", title: "Mac Address of Module" 
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    input 'logInfo', 'bool', title: 'Show Info Logs?',  required: false, defaultValue: true
    input 'logWarn', 'bool', title: 'Show Warning Logs?', required: false, defaultValue: true
    input 'logDebug', 'bool', title: 'Show Debug Logs?', description: 'Only leave on when required', required: false, defaultValue: true
    input 'logTrace', 'bool', title: 'Show Detailed Logs?', description: 'Only leave on when required', required: false, defaultValue: true

    //help guide
    input name: "UserGuide", type: "hidden", title: fmtHelpInfo("Manual do Driver") 

//*** Control Remote Commands - Verificar *** //
	input name: "OnIRsend", title:"On", type: "string"
	input name: "OffIRsend", title:"Off", type: "string"
    input name: "muteIRsend", title:"Mute(2)", type: "string"  
	input name: "sourceIRsend", title:"Source(3)", type: "string"	
    input name: "backIRsend", title:"Back(4)", type: "string"  
    input name: "menuIRsend", title:"Menu(5)", type: "string"	
    input name: "hdmi1IRsend", title:"Hdmi1(6)", type: "string"
	input name: "hdmi2IRsend", title:"Hdmi2(7)", type: "string"
	input name: "upIRsend", title:"Up(8)", type: "string"
	input name: "downIRsend", title:"Down(9)", type: "string"
	input name: "rightIRsend", title:"Right(10)", type: "string"
	input name: "leftIRsend", title:"Left(11)", type: "string"
	input name: "confirmIRsend", title:"Confirm(12)", type: "string"      
    input name: "exitIRsend", title:"Exit(13)", type: "string"  
    input name: "homeIRsend", title:"Home(14)", type: "string"  
    input name: "ChanUpIRsend", title:"Channel Up(18)", type: "string"
 	input name: "ChanDownIRsend", title:"Channel Down(19)", type: "string"
 	input name: "VolUpIRsend", title:"Volume Up(21)", type: "string"
 	input name: "VolDownIRsend", title:"Volume Down(22)", type: "string"
	input name: "num0IRsend", title:"Num0(23)", type: "string"
    input name: "num1IRsend", title:"Num1(24)", type: "string"
	input name: "num2IRsend", title:"Num2(25)", type: "string"
	input name: "num3IRsend", title:"Num3(26)", type: "string"
	input name: "num4IRsend", title:"Num4(27)", type: "string"
	input name: "num5IRsend", title:"Num5(28)", type: "string"
	input name: "num6IRsend", title:"Num6(29)", type: "string"
	input name: "num7IRsend", title:"Num7(30)", type: "string"
	input name: "num8IRsend", title:"Num8(31)", type: "string"
	input name: "num9IRsend", title:"Num9(32)", type: "string"
    input name: "btnextra1IRsend", title:"Botonextra1(33)", type: "string"  
    input name: "btnextra2IRsend", title:"Botonextra2(34)", type: "string"        
    input name: "btnextra3IRsend", title:"Botonextra2(35)", type: "string"     
    input name: "netflixIRsend", title:"Netflix(38)", type: "string"
	input name: "amazonIRsend", title:"Amazon Prime(39)", type: "string" 
    input name: "youtubeIRsend", title:"Youtube(40)", type: "string"             
    //input name: "repeatSendHEX", title:"Repeat for SendHex", type: "string", defaultValue: "1"   // REPEAT SEND PRONTO HEX
         
      
      
  }   

@Field static String partialMessage = ''
@Field static Integer checkInterval = 150

def installed() {
    logTrace('installed()')
    boardstatusvar = "offline"
    runIn(1800, logsOff)
} //OK
    
def uninstalled() {
    logTrace('uninstalled()')
    unschedule()
    interfaces.rawSocket.close()
}

def updated() {
    logTrace('updated()')
}



def reconnect () {
    interfaces.rawSocket.close();
    state.lastMessageReceived = ""
    state.lastmessage = ""


    try {
        logTrace("tentando conexão com o device no ${device_IP_address}...na porta ${device_port}");
        interfaces.rawSocket.connect(device_IP_address, (int) device_port);
        state.lastMessageReceivedAt = now();
        runIn(checkInterval, "connectionCheck");
    }
    catch (e) {
         logError( "${device_IP_address} keepalive error: ${e.message}" )
         sendEvent(name: "boardstatus", value: "offline", isStateChange: true)        
    }
    pauseExecution(500)    
    getmac()
    
}
 

def keepalive() {
    logTrace('keepalive()')
    unschedule()
    state.lastMessageReceived = ""
    state.lastmessage = ""
    interfaces.rawSocket.close();
    
    try {
        logTrace("tentando conexão com o device no ${device_IP_address}...na porta ${device_port}");
        interfaces.rawSocket.connect(device_IP_address, (int) device_port);
        state.lastMessageReceivedAt = now();
        runIn(checkInterval, "connectionCheck");
    }
    catch (e) {
         logError( "${device_IP_address} keepalive error: ${e.message}" )
         sendEvent(name: "boardstatus", value: "offline", isStateChange: true)        
    }
    pauseExecution(500)    
    
    
}


def initialize() {
    unschedule()
    logTrace('Run Initialize()')
    interfaces.rawSocket.close();    
    boardstatusvar = "offline"    
    
    if (!device_IP_address) {
        logError 'IP do Device not configured'
        return
    }

    if (!device_port) {
        logError 'Porta do Device não configurada.'
        return
    }
    
    
    try {
        logTrace("Initialize: Tentando conexão com o device no ${device_IP_address}...na porta configurada: ${device_port}");
        interfaces.rawSocket.connect(device_IP_address, (int) device_port);
        state.lastMessageReceivedAt = now();        
        if (boardstatusvar == "offline") { 
            sendEvent(name: "boardstatus", value: "online", isStateChange: true)    
            boardstatusvar = "online"
        }
        boardstatusvar = "online"
        runIn(checkInterval, "connectionCheck");
        
    }
    catch (e) {
        logError( "Initialize: com ${device_IP_address} com um error: ${e.message}" )
        boardstatusvar = "offline"
        runIn(60, "initialize");
    }    
       runIn(10, "refresh");
    
}



def refresh() {
    def msg = "mdcmd_getmd," + state.newmacdec
    logTrace('Sent refresh()')   
    EnviaComando(msg)
}


////////////////////////////
//// Connections Checks ////
////////////////////////////

def connectionCheck() {
    def now = now();
    
    if ( now - state.lastMessageReceivedAt > (checkInterval * 1000)) { 
        logError("sem mensagens desde ${(now - state.lastMessageReceivedAt)/60000} minutos, reconectando ...");
        keepalive();
    }
    else if (state.lastmessage.contains("ParseError")){
        logError("Problemas no último Parse, reconectando ...");
        keepalive();
    } else {       
        logDebug("Connection Check = ok - Board response. ");
        sendEvent(name: "boardstatus", value: "online")
        runIn(checkInterval, "connectionCheck");
    }
}



private EnviaComando(s) {
    logDebug("sendingCommand ${s}")
    interfaces.rawSocket.sendMessage(s)    
}


//Basico on e off para Switch 
def on() {
     sendEvent(name: "switch", value: "on", isStateChange: true)
     def ircode =  (settings.OnIRsend ?: "")
     EnviaComando(ircode)

}

def off() {
     sendEvent(name: "switch", value: "off", isStateChange: true)
     def ircode =  (settings.OffIRsend ?: "")    
     EnviaComando(ircode)
         
}


def parse(msg) {
    state.lastMessageReceived = new Date(now()).toString();
    state.lastMessageReceivedAt = now();
        
    def oldmessage = state.lastmessage
    
    def newmsg = hubitat.helper.HexUtils.hexStringToByteArray(msg) //na Mol, o resultado vem em HEX, então preciso converter para Array
    def newmsg2 = new String(newmsg) // Array para String    
    
    state.lastmessage = newmsg2 //ok
    larguramsg = newmsg2.length()
    state.larguramsg = larguramsg
    log.info "qtde chars = " + larguramsg
    log.info "lastmessage = " + newmsg2
    
    //completeir,1:8,1
    //qtde chars = 18

    //IR Return 
    if ((newmsg2.contains("completeir") && (newmsg2.length() == 18))) {
        //log.info "mac completa = " + newmsg2

                
        log.info "Enviado OK o Comando IR "
        sendEvent(name: "boardstatus", value: "online")

    }    
    
    
    
    
    

}


//Case para los botones de push en el dashboard. 
def push(pushed) {
	logDebug("push: button = ${pushed}")
	if (pushed == null) {
		logWarn("push: pushed is null.  Input ignored")
		return
	}
	pushed = pushed.toInteger()
	switch(pushed) {
		case 1 : poweron(); break
        case 2 : mute(); break
		case 3 : source(); break
		case 4 : back(); break
        case 5 : menu(); break
        case 6 : hdmi1(); break
        case 7 : hdmi2(); break                
		case 8 : left(); break
		case 9 : right(); break
		case 10: up(); break
		case 11: down(); break
		case 12: confirm(); break
		case 13: exit(); break
		case 14: home(); break
		case 18: channelUp(); break
		case 19: channelDown(); break
		case 21: volumeUp(); break
		case 22: volumeDown(); break
		case 23: num0(); break
		case 24: num1(); break
		case 25: num2(); break
		case 26: num3(); break
    	case 27: num4(); break        
		case 28: num5(); break
    	case 29: num6(); break
	    case 30: num7(); break
    	case 31: num8(); break            
	    case 32: num9(); break   
	    case 33: btnextra1(); break                
		case 34: btnextra2(); break
		case 35: btnextra3(); break
		case 38: appAmazonPrime(); break
		case 39: appYouTube(); break
		case 40: appNetflix(); break    
		case 41: poweroff(); break            
		default:
			logDebug("push: Botão inválido.")
			break
	}
}

//Botão #1 para dashboard
def poweron(){
     sendEvent(name: "switch", value: "on", isStateChange: true)
     def ircode =  (settings.OnIRsend ?: "")
     EnviaComando(ircode)  
}

//Botão #41 para dashboard
def poweroff(){
     sendEvent(name: "switch", value: "off", isStateChange: true)
     def ircode =  (settings.OffIRsend ?: "")
     EnviaComando(ircode)  
}


//Botão #2 para dashboard
def mute(){
	sendEvent(name: "volume", value: "mute")
    def ircode =  (settings.muteIRsend ?: "")
    EnviaComando(ircode)    
}


//Botão #3 para dashboard
def source(){
	sendEvent(name: "action", value: "source")
    def ircode =  (settings.sourceIRsend ?: "")
    EnviaComando(ircode)    
}

//Botão #4 para dashboard
def back(){
	sendEvent(name: "action", value: "back")
    def ircode =  (settings.backIRsend ?: "")
    EnviaComando(ircode)    
}

//Botão #5 para dashboard
def menu(){
	sendEvent(name: "action", value: "menu")
    def ircode =  (settings.menuIRsend ?: "")
    EnviaComando(ircode)    
}


//Botão #6 para dashboard
def hdmi1(){
    sendEvent(name: "input", value: "hdmi1")
    def ircode =  (settings.hdmi1IRsend ?: "")
    EnviaComando(ircode)
}

//Botão #7 para dashboard
def hdmi2(){
    sendEvent(name: "input", value: "hdmi2")
    def ircode =  (settings.hdmi2IRsend ?: "")
    EnviaComando(ircode)
}



//Botão #8 para dashboard
def left(){
    sendEvent(name: "action", value: "left")
    def ircode =  (settings.btnextra1IRsend ?: "")
    EnviaComando(ircode)
}

//Botão #9 para dashboard
def right(){
    sendEvent(name: "action", value: "right")
     def ircode =  (settings.btnextra1IRsend ?: "")
    EnviaComando(ircode)
}



//Botão #10 para dashboard
def up(){
    sendEvent(name: "action", value: "up")
    def ircode =  (settings.upIRsend ?: "")
    EnviaComando(ircode)
}

//Botão #11 para dashboard
def down(){
    sendEvent(name: "action", value: "down")
    def ircode =  (settings.hdmi1 ?: "")
    EnviaComando(ircode)
}

//Botão #12 para dashboard
def confirm(){
    sendEvent(name: "action", value: "confirm")
    def ircode =  (settings.confirmIRsend ?: "")
    EnviaComando(ircode)
}


//Botão #13 para dashboard
def exit(){
	sendEvent(name: "action", value: "exit")
    def ircode =  (settings.exitIRsend ?: "")
    EnviaComando(ircode)    
}




//Botão #14 para dashboard
def home(){
    sendEvent(name: "action", value: "home")
    def ircode =  (settings.homeIRsend ?: "")
    EnviaComando(ircode)
}



//Botão #18 para dashboard
def channelUp(){
	sendEvent(name: "channel", value: "chup")
   def ircode =  (settings.ChanUpIRsend ?: "")
    EnviaComando(ircode)    
}

//Botão #19 para dashboard
def channelDown(){
	sendEvent(name: "channel", value: "chdown")
    def ircode =  (settings.ChanDownIRsend ?: "")
    EnviaComando(ircode)    
}

//Botão #21 para dashboard
def volumeUp(){
	sendEvent(name: "volume", value: "volup")
    def ircode =  (settings.VolUpIRsend ?: "")
    EnviaComando(ircode)    
}

//Botão #22 para dashboard
def volumeDown(){
	sendEvent(name: "volume", value: "voldown")
    def ircode =  (settings.VolDownIRsend ?: "")
    EnviaComando(ircode)    
}


//Botão #23 para dashboard
def num0(){
    sendEvent(name: "action", value: "num0")
    def ircode =  (settings.num0IRsend ?: "")
    EnviaComando(ircode)
}

//Botão #24 para dashboard
def num1(){
    sendEvent(name: "action", value: "num1")
   def ircode =  (settings.num1IRsend ?: "")
    EnviaComando(ircode)
}

//Botão #25 para dashboard
def num2(){
    sendEvent(name: "action", value: "num2")
    def ircode =  (settings.num2IRsend ?: "")
    EnviaComando(ircode)
}


//Botão #26 para dashboard
def num3(){
    sendEvent(name: "action", value: "num3")
    def ircode =  (settings.num3IRsend ?: "")
    EnviaComando(ircode)
}

//Botão #27 para dashboard
def num4(){
    sendEvent(name: "action", value: "num4")
    def ircode =  (settings.num4IRsend ?: "")
    EnviaComando(ircode)
}

//Botão #28 para dashboard
def num5(){
    sendEvent(name: "action", value: "num5")
    def ircode =  (settings.num5IRsend ?: "")
    EnviaComando(ircode)
}

//Botão #29 para dashboard
def num6(){
    sendEvent(name: "action", value: "num6")
    def ircode =  (settings.num6IRsend ?: "")
    EnviaComando(ircode)
}


//Botão #30 para dashboard
def num7(){
    sendEvent(name: "action", value: "num7")
    def ircode =  (settings.num7IRsend ?: "")
    EnviaComando(ircode)
}

//Botão #31 para dashboard
def num8(){
    sendEvent(name: "action", value: "num8")
    def ircode =  (settings.num8IRsend ?: "")
    EnviaComando(ircode)
}

//Botão #32 para dashboard
def num9(){
    sendEvent(name: "action", value: "num9")
    def ircode =  (settings.num9IRsend ?: "")
    EnviaComando(ircode)
}

//Botão #33 para dashboard
def btnextra1(){
    sendEvent(name: "action", value: "confirm")
    def ircode =  (settings.btnextra1IRsend ?: "")
    EnviaComando(ircode)
}

//Botão #34 para dashboard
def btnextra2(){
    sendEvent(name: "action", value: "btnextra2")
    def ircode =  (settings.btnextra2IRsend ?: "")
    EnviaComando(ircode)
}

//Botão #35 para dashboard
def btnextra3(){
    sendEvent(name: "action", value: "btnextra3")
    def ircode =  (settings.btnextra3IRsend ?: "")
    EnviaComando(ircode)
}

//Botão #38 para dashboard
def appAmazonPrime(){
    sendEvent(name: "input", value: "amazon")
    def ircode =  (settings.amazonIRsend ?: "")
    EnviaComando(ircode)
}

//Botão #39 para dashboard
def appyoutube(){
    sendEvent(name: "input", value: "youtube")
   def ircode =  (settings.youtubeIRsend ?: "")
    EnviaComando(ircode)
}


//Botão #40 para dashboard
def appnetflix(){
    sendEvent(name: "input", value: "netflix")
    def ircode =  (settings.netflixIRsend ?: "")
    EnviaComando(ircode)
}
  


def logsOff() {
    log.warn 'logging disabled...'
    device.updateSetting('logInfo', [value:'false', type:'bool'])
    device.updateSetting('logWarn', [value:'false', type:'bool'])
    device.updateSetting('logDebug', [value:'false', type:'bool'])
    device.updateSetting('logTrace', [value:'false', type:'bool'])
}

void logDebug(String msg) {
    if ((Boolean)settings.logDebug != false) {
        log.debug "${drvThis}: ${msg}"
    }
}

void logInfo(String msg) {
    if ((Boolean)settings.logInfo != false) {
        log.info "${drvThis}: ${msg}"
    }
}

void logTrace(String msg) {
    if ((Boolean)settings.logTrace != false) {
        log.trace "${drvThis}: ${msg}"
    }
}

void logWarn(String msg, boolean force = false) {
    if (force || (Boolean)settings.logWarn != false) {
        log.warn "${drvThis}: ${msg}"
    }
}

void logError(String msg) {
    log.error "${drvThis}: ${msg}"
}





