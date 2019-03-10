package dev.tempest.zapmatch;

import android.media.SoundPool;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;



public class Level extends AppCompatActivity {

    LinearLayout infoLayout;
    LinearLayout gameLayout;
    ProgressBar powerChargeBar = null;
    TextView turnsText;
    TextView levelText;
    TextView scoreText;
    TextView targetText;
    LinearLayout turnsLevelRow;
    LinearLayout scoreRow;
    Button soundButton;
    JSONObject data;

    public Level(){

    }
    static class LevelInfo {
        static int currentTurns = 0;
        static int currentScore = 0;
        static int targetScore = 0;
        static int gridSize = 0;
        static JSONArray levelArray;
        static boolean powerSoundPlayed = false;
        static JSONArray grid;
        static int gridItemStaticSoundId;
        static int gridItemStaticFailSoundId;
        static int matchMadeSoundId;
        static int powerActivatedSoundId;
        static int powerDeactivatedSoundId;
        static int powerReadySoundId;
        static int powerUsedSoundId;
        static int winSoundId;
        static int loseSoundId;
        static int menuClickId;
        static SoundPool soundPool;

    }
}
