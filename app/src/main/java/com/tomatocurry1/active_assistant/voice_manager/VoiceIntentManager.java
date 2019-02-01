package com.tomatocurry1.active_assistant.voice_manager;

import android.os.CancellationSignal;

import com.tomatocurry1.active_assistant.FloatingViewService;

public class VoiceIntentManager {

    public enum VoiceIntents {CALL, DUMMMY;}

    final FloatingViewService service;


    public VoiceIntentManager(FloatingViewService service){
        this.service = service;
    }

    public VoiceIntents detectIntents(String text){
        text = text.toLowerCase();
        if (text.contains("call")){
            return VoiceIntents.CALL;
        }




        return VoiceIntents.DUMMMY;
    }

    public CancellationSignal handleSpeech(String text){
        if (text == null)
            return null;
        switch(detectIntents(text)){
            case CALL:
                CallManager callManager = new CallManager();
                CancellationSignal cancellationSignal = new CancellationSignal();
                if (callManager.readyCall(service, text, cancellationSignal)){

                    service.displaySpeechBubble("Starting a call to: " + text.substring(5));
                    service.assignSpeechBubbleCancel(cancellationSignal);
                    return cancellationSignal;
                }else{
                    service.displaySpeechBubble("No contact was found");
                }
                break;

            case DUMMMY:
        }
        return null;
    }

    public boolean handleCall(){
        return false;
    }

}
