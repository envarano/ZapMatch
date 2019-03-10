package dev.tempest.zapmatch;

import java.util.LinkedHashMap;

class Achievement {


    LinkedHashMap<String, Boolean> list = new LinkedHashMap<>();

    Achievement(){
        list.put("master", false);
        list.put("noSweat", false);
        list.put("unstoppable", false);
        list.put("perfectionist", false);
        list.put("halfLife3Confirmed", false);
        list.put("amIDoingItRight", false);
        list.put("highAchiever", false);
        list.put("EZ", false);
    }


}
