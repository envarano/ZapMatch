package dev.tempest.zapmatch;

import android.content.Intent;
import android.content.res.Configuration;
import android.media.SoundPool;
import android.support.v7.app.AppCompatActivity;
import dev.tempest.zapmatch.Player.PlayerInfo;
import dev.tempest.zapmatch.R;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.google.android.gms.ads.MobileAds;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    SoundPool sp;
    PlayerInfo pi = new PlayerInfo();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.print("resetting PlayerInfo: ");
        System.out.println(pi);
        MobileAds.initialize(this, "ca-app-pub-6086432284953891~9274629725");
        File file = new File(this.getFilesDir(), "player_info");
        sp = new SoundPool.Builder().setMaxStreams(5).build();
        Level.LevelInfo.menuClickId = sp.load(this, R.raw.match_made, 1);
        FileInputStream is;
        try {
            is = openFileInput(file.getName());
            StringBuffer fileContent = new StringBuffer();
            InputStreamReader isr = new InputStreamReader(is) ;
            BufferedReader buffReader = new BufferedReader(isr) ;
            String readString = buffReader.readLine() ;

            while (readString != null)
            {
                fileContent.append(readString);
                readString = buffReader.readLine();
            }

            String unfilteredPlayerData = String.valueOf(fileContent);
            String[] playerData = unfilteredPlayerData.split(",");

            PlayerInfo.level = Integer.parseInt(playerData[0]);
            PlayerInfo.levelHighest = Integer.parseInt(playerData[1]);

            for(int i = 0; i < PlayerInfo.achievements.size(); i++){
                StringBuilder objectConverter = new StringBuilder();
                objectConverter.append(Objects.requireNonNull(PlayerInfo.achievements.keySet().toArray())[i]);
                String stringBoolean = String.valueOf(objectConverter);
                PlayerInfo.achievements.put(stringBoolean, Boolean.parseBoolean(playerData[i+2]));
            }



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void loadChallengeMode(View view) {
        sp.play(Level.LevelInfo.menuClickId, PlayerInfo.soundLevel, PlayerInfo.soundLevel, 0, 0, 1);
        PlayerInfo.isEndless = false;
        Intent intent = new Intent(this, GameScreen.class);
        startActivity(intent);
    }

    public void loadEndlessMode(View view) {
        sp.play(Level.LevelInfo.menuClickId, PlayerInfo.soundLevel, PlayerInfo.soundLevel, 0, 0, 1);
        PlayerInfo.isEndless = true;
        Intent intent = new Intent(this, GameScreen.class);
        startActivity(intent);
    }

    public void loadAchievementsMenu(View view){
        sp.play(Level.LevelInfo.menuClickId, PlayerInfo.soundLevel, PlayerInfo.soundLevel, 0, 0, 1);
        Intent intent = new Intent(this, AchievementsScreen.class);
        startActivity(intent);
    }

    public void loadLevelMenuEasy(View view){
        sp.play(Level.LevelInfo.menuClickId, PlayerInfo.soundLevel, PlayerInfo.soundLevel, 0, 0, 1);
        PlayerInfo.isEndless = false;
        Intent intent = new Intent(this, LevelMenu.class);
        startActivity(intent);
    }

    public void muteSound(View view){

        Button soundButton = findViewById(R.id.sound_button);

        if(PlayerInfo.soundLevel == 1) {
            PlayerInfo.soundLevel = 0;
            soundButton.setBackground(getDrawable(R.drawable.sound_muted));
        }
        else{
            PlayerInfo.soundLevel = 1;
            soundButton.setBackground(getDrawable(R.drawable.sound));
        }
    }

    public void loadInfo(View view){

        Intent intent = new Intent(this, InfoScreen.class);
        startActivity(intent);
    }

    @Override
    public void onResume(){
        super.onResume();
        Button levelButton = findViewById(R.id.levelButton);
        levelButton.setText(String.valueOf(Player.PlayerInfo.level + 1));
        Button soundButton = findViewById(R.id.sound_button);

        if (PlayerInfo.soundLevel == 1) {

            soundButton.setBackground(getDrawable(R.drawable.sound));

        } else {
            soundButton.setBackground(getDrawable(R.drawable.sound_muted));
        }
    }
}
