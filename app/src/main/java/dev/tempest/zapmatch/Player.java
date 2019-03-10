package dev.tempest.zapmatch;

import java.util.LinkedHashMap;

class Player {

    static class PlayerInfo {
        static Achievement achievement;
        static LinkedHashMap<String, Boolean> achievements = new LinkedHashMap<>();
        static boolean isEndless = false;
        static int level = 0;
        static int levelHighest = 0;
        static boolean levelPassed = false;
        static int powerCharge = 0;
        static int soundLevel = 1;
        static boolean powerActivated = false;
        static boolean wantsTip = true;
        static Power power = Power.OVERRULE;

        enum Power{
            NORMALIZE, DESTRUCT, OVERFLOW, OVERRULE, OVERLOAD
        }

        PlayerInfo() {
            achievement = new Achievement();
            achievements = achievement.list;
        }
    }
}
