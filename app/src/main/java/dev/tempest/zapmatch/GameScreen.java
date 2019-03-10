package dev.tempest.zapmatch;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import dev.tempest.zapmatch.Player.PlayerInfo;
import dev.tempest.zapmatch.Level.LevelInfo;
import dev.tempest.zapmatch.R;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.SoundPool;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintLayout.LayoutParams;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import static dev.tempest.zapmatch.Player.PlayerInfo.Power.OVERRULE;

public class GameScreen extends AppCompatActivity {

    Level levelUI;
    private static final float ITEM_MARGIN_MODIFIER = 0.05f;
    private static final float ITEM_MARGIN_MODIFIER_TOP = 0.1f;
    private static final float GAME_MARGIN_MODIFIER = 0.025f;
    //final float DENSITY_SCALE = this.getResources().getDisplayMetrics().density;

    @SuppressLint({"RestrictedApi", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //build soundPool
        LevelInfo.soundPool = new SoundPool.Builder().setMaxStreams(5).build();
        //load sounds
        //LevelInfo.gridItemStaticSoundId = LevelInfo.soundPool.load(this, R.raw.matchable, 1);
        LevelInfo.matchMadeSoundId = LevelInfo.soundPool.load(this, R.raw.match_made, 1);
        LevelInfo.gridItemStaticFailSoundId = LevelInfo.soundPool.load(this, R.raw.match_two, 1);
        LevelInfo.powerActivatedSoundId = LevelInfo.soundPool.load(this, R.raw.power_up_two_trimmed, 1);
        LevelInfo.powerDeactivatedSoundId = LevelInfo.soundPool.load(this, R.raw.power_up_three_trimmed, 1);
        LevelInfo.powerReadySoundId = LevelInfo.soundPool.load(this, R.raw.power_up_ready, 1);
        LevelInfo.powerUsedSoundId = LevelInfo.soundPool.load(this, R.raw.match_three, 1);
        LevelInfo.winSoundId = LevelInfo.soundPool.load(this, R.raw.win_one, 1);
        LevelInfo.loseSoundId = LevelInfo.soundPool.load(this, R.raw.lose, 1);

        //reset power-ups charge
        PlayerInfo.powerCharge = 0;
        //reset players score
        LevelInfo.currentScore = 0;

        if (PlayerInfo.level == 17){
            LevelInfo.currentScore = 100;
        }
        //show game screen

        //assign UI references
        levelUI = new Level();

        setContentView(R.layout.game_screen);
        levelUI.gameLayout = findViewById(R.id.game_layout);
        levelUI.infoLayout = findViewById(R.id.info_ui_layout);
        levelUI.turnsText = findViewById(R.id.turns_text);
        levelUI.levelText = findViewById(R.id.level_text);
        levelUI.scoreText = findViewById(R.id.score_text);
        levelUI.targetText = findViewById(R.id.target_text);
        levelUI.powerChargeBar = findViewById(R.id.charge_progress_bar);
        levelUI.turnsLevelRow = (LinearLayout) levelUI.turnsText.getParent();
        levelUI.scoreRow = (LinearLayout) levelUI.scoreText.getParent();
        levelUI.soundButton = findViewById(R.id.sound_button);

        AdView mAdView = findViewById(R.id.adView);
        if(mAdView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }

        //draw sound icon as regular or muted depending on whether user has already muted before starting game
        if (PlayerInfo.soundLevel == 1) {
            levelUI.soundButton.setBackground(getDrawable(R.drawable.sound));

        } else {
            levelUI.soundButton.setBackground(getDrawable(R.drawable.sound_muted));
        }

        //remove UI elements if endless mode is on
        if(PlayerInfo.isEndless){
            PlayerInfo.level = 0;
            levelUI.turnsLevelRow.removeView(levelUI.turnsText);
            levelUI.turnsLevelRow.removeView(levelUI.levelText);
            levelUI.scoreRow.removeView(levelUI.targetText);
        }

        levelUI.data = getLevels();

        try {
            //assign level info from JSON
            LevelInfo.grid = levelUI.data.getJSONArray("value");
            LevelInfo.currentTurns = levelUI.data.getInt("turns");
            LevelInfo.targetScore = levelUI.data.getInt("score");

            //update text
            updateUI();

            Typeface face = Typeface.createFromAsset(getAssets(),
                    "font/akashi.ttf");

            //if the square root of the total number of values is an integer, our grid will be evenly sized
            if (Math.sqrt(LevelInfo.grid.length()) == (int) Math.sqrt(LevelInfo.grid .length())) {

                //assign square root as grid size
                LevelInfo.gridSize = (int) Math.sqrt(LevelInfo.grid.length());
            }

            //get display metrics for dynamic grid sizing
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int width = displayMetrics.widthPixels;

            //set grid items to be 80% of screen width
            int newWidth = (int) (width * 0.8) / (LevelInfo.gridSize);
            int newHeight = (int) (width * 0.8) / (LevelInfo.gridSize);
            int textSize = (int) (newWidth / 1.5);

            //assign info UI params
            levelUI.infoLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(newWidth, newHeight);

            //create grid rows
            for (int i = 0; i < LevelInfo.gridSize; i++) {

                LinearLayout row = new LinearLayout(this);
                row.setLayoutParams(params);
                row.setOrientation(LinearLayout.HORIZONTAL);

                //fill grid rows
                for (int j = 0; j < LevelInfo.gridSize; j++) {

                    GridTextView gridItem = new GridTextView(this);
                    gridItem.setText(String.format(Locale.getDefault(), "%d", LevelInfo.grid.getInt(j + (i * LevelInfo.gridSize))));
                    gridItem.setTextColor(Color.parseColor("#1f1f2a"));
                    //center text
                    gridItem.setGravity(Gravity.CENTER);
                    gridItem.setTextSize(textSize);
                    gridItem.setAutoSizeTextTypeUniformWithConfiguration(textSize / 3, textSize, 15, TypedValue.COMPLEX_UNIT_PX);
                    gridItem.setMaxLines(1);
                    //gridItem.setAutoSizeTextTypeUniformWithConfiguration((textSize / 2), (textSize) * 2, 2, TypedValue.COMPLEX_UNIT_SP);
                    gridItem.setTypeface(face);
                    gridItem.setLayoutParams(itemParams);
                    itemParams.leftMargin = (int) ((width * ITEM_MARGIN_MODIFIER) / LevelInfo.gridSize);
                    itemParams.rightMargin = (int) ((width * ITEM_MARGIN_MODIFIER) / LevelInfo.gridSize);
                    itemParams.topMargin = (int) ((width * ITEM_MARGIN_MODIFIER_TOP) / LevelInfo.gridSize);
                    gridItem.setBackground(getDrawable(R.drawable.grid_item));
                    //add colour filter to background for match colouring
                    gridItem.getBackground().setColorFilter(Color.rgb(gridItem.redInBackground, gridItem.greenInBackground, gridItem.blueInBackground), PorterDuff.Mode.OVERLAY);
                    //reference of location
                    gridItem.xLocation = j;
                    gridItem.yLocation = i;
                    gridItem.setOnTouchListener(new gridTouchListener());
                    gridItem.setOnDragListener(new gridDragListener());
                    updateGridItemColour(gridItem);
                    //add item to row
                    row.addView(gridItem);
                    //stops text views moving down as text gets smaller
                    row.setBaselineAligned(false);
                }

                levelUI.gameLayout.addView(row);
            }

            LayoutParams paramsGameLayout = (LayoutParams) levelUI.gameLayout.getLayoutParams();
            paramsGameLayout.leftMargin = (int) (width * GAME_MARGIN_MODIFIER);
            paramsGameLayout.rightMargin = (int) (width * GAME_MARGIN_MODIFIER);
            paramsGameLayout.bottomMargin = (int) (width * GAME_MARGIN_MODIFIER);

            levelUI.gameLayout.setPadding(
                    (int) (width * GAME_MARGIN_MODIFIER),
                    (int) (width * GAME_MARGIN_MODIFIER),
                    (int) (width * GAME_MARGIN_MODIFIER),
                    (int) (width * GAME_MARGIN_MODIFIER));

            levelUI.gameLayout.setLayoutParams(paramsGameLayout);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        LinearLayout chargeBar = findViewById(R.id.charge_bar_layout);
        LinearLayout chargeBarParent = (LinearLayout) chargeBar.getParent();

        //if its a level that has tips
        if(PlayerInfo.level < 4 && !PlayerInfo.isEndless) {
            //create tip layout
            chargeBarParent.removeView(chargeBar);
            final LinearLayout tipScreen = new LinearLayout(this);
            LinearLayout okBar = new LinearLayout(this);
            tipScreen.setElevation(100);
            CheckBox tipCB = new CheckBox(this);
            TextView CBText = new TextView(this);
            CBText.setText(getString(R.string.dont_show_message));
            TextView tipText = new TextView(this);
            Button closeButton = new Button(this);
            closeButton.setText("");
            closeButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            closeButton.setGravity(Gravity.END);
            closeButton.setTextColor(Color.parseColor("#dddddd"));
            int states[][] = {{android.R.attr.state_checked}, {}};
            int colors[] = {Color.parseColor("#dddddd"), Color.parseColor("#dddddd")};
            CompoundButtonCompat.setButtonTintList(tipCB, new ColorStateList(states, colors));
            CBText.setTextColor(Color.parseColor("#dddddd"));
            closeButton.setBackground(getDrawable(R.drawable.close_drawable));
            Button okButton = new Button(this);
            okButton.setText(getString(R.string.ok_button));
            okButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            okButton.setGravity(Gravity.CENTER);
            CBText.setGravity(Gravity.CENTER);
            tipCB.setGravity(Gravity.CENTER);
            okButton.setTextColor(Color.parseColor("#dddddd"));
            okButton.setBackground(getDrawable(R.drawable.value_text_background));
            LinearLayout tipLL = new LinearLayout(this);
            tipLL.setOrientation(LinearLayout.VERTICAL);
            tipLL.addView(tipText);
            tipLL.addView(okButton);
            tipLL.addView(okBar);
            tipScreen.addView(tipLL);
            tipText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            tipText.setTextColor(Color.parseColor("#dddddd"));
            tipText.setGravity(Gravity.CENTER);
            okBar.addView(tipCB);
            okBar.addView(CBText);

            tipCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    PlayerInfo.wantsTip = !PlayerInfo.wantsTip;
                }
            });
            //show tip if player has not chosen to stop tips
            if(PlayerInfo.wantsTip) {

                tipScreen.setBackground(getDrawable(R.drawable.tip_background));
                tipScreen.setClickable(true);
                tipScreen.setId(View.generateViewId());
                ConstraintLayout constraintLayout = findViewById(R.id.game_constraint_layout);
                constraintLayout.addView(tipScreen);

                okButton.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View v) {
                        ((ViewGroup) tipScreen.getParent()).removeView(tipScreen);
                    }
                });

                LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                float density = this.getResources().getDisplayMetrics().density;
                int tipMargin = (int) (9 * density + 0.5f);
                params.setMargins(tipMargin, tipMargin, tipMargin, tipMargin);
                tipScreen.setLayoutParams(params);
                ConstraintSet cs = new ConstraintSet();
                cs.clone(constraintLayout);
                cs.connect(tipScreen.getId(), ConstraintSet.BOTTOM, levelUI.gameLayout.getId(), ConstraintSet.TOP, tipMargin);
                cs.applyTo(constraintLayout);

                switch (PlayerInfo.level) {
                    case 0:
                        tipText.setText(getString(R.string.tip_one));
                        break;

                    case 1:
                        tipText.setText(getString(R.string.tip_two));
                        break;

                    case 2:
                        tipText.setText(getString(R.string.tip_three));
                        break;

                    case 3:
                        tipText.setText(getString(R.string.tip_four));
                        break;
                }

            }

        }
        //if player is passed level 3 then add the charge bar back
        if(PlayerInfo.level >= 3 && chargeBar.getParent() == null){
            chargeBarParent.addView(chargeBar);
        }
    }
    private final class gridTouchListener implements OnTouchListener {

        @SuppressLint({"ClickableViewAccessibility"})
        @Override
        //start drag
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                ClipData data = ClipData.newPlainText("", "");
                DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                v.startDragAndDrop(data, shadowBuilder, v, 0);
                return true;
            }
            return false;
        }

    }
    private final class gridDragListener implements OnDragListener {

        @Override
        public boolean onDrag(View v, DragEvent event) {
            GridTextView movingSquare = (GridTextView) event.getLocalState();
            GridTextView goalSquare;
            final int action = event.getAction();

            switch (action) {
                case DragEvent.ACTION_DRAG_STARTED:

                    if (event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                        //play sound and update background
                        LevelInfo.soundPool.play(LevelInfo.gridItemStaticSoundId, PlayerInfo.soundLevel, PlayerInfo.soundLevel, 0, 0, 1);
                        updateBackgroundAndFilter(getDrawable(R.drawable.grid_item_good), movingSquare);
                        return true;

                    }
                    return false;

                case DragEvent.ACTION_DRAG_ENTERED:

                    goalSquare = (GridTextView) v;
                    int nextMatchNumber = movingSquare.matches + 1;

                    //highlight matching numbers and get a reference to included items in a match
                    switch (movingSquare.matches) {

                        case 0:
                            if (matchCanBeMadeWith(movingSquare, movingSquare, goalSquare, nextMatchNumber)) {
                                updateBackgroundAndFilter(getDrawable(R.drawable.grid_item_good), movingSquare);
                                updateBackgroundAndFilter(getDrawable(R.drawable.grid_item_good), goalSquare);
                                //references to matching items
                                movingSquare.firstMatch = goalSquare.getValue();
                                movingSquare.firstMatchObject = goalSquare;
                                movingSquare.matches++;
                                //matching sound
                                LevelInfo.soundPool.play(LevelInfo.gridItemStaticSoundId, PlayerInfo.soundLevel, PlayerInfo.soundLevel, 0, 0, 1);
                                return true;
                            } else {
                                if (goalSquare != movingSquare) {
                                    updateBackgroundAndFilter(getDrawable(R.drawable.grid_item_bad), goalSquare);
                                    LevelInfo.soundPool.play(LevelInfo.gridItemStaticFailSoundId, PlayerInfo.soundLevel, PlayerInfo.soundLevel, 0, 0, 1);
                                }
                            }
                            break;

                        case 1:

                            if (matchCanBeMadeWith(movingSquare, movingSquare.firstMatchObject, goalSquare, nextMatchNumber)) {
                                updateBackgroundAndFilter(getDrawable(R.drawable.grid_item_good), goalSquare);
                                //references to matching items
                                movingSquare.secondMatch = goalSquare.getValue();
                                movingSquare.secondMatchObject = goalSquare;
                                movingSquare.matches++;
                                //matching sound
                                LevelInfo.soundPool.play(LevelInfo.gridItemStaticSoundId, PlayerInfo.soundLevel, PlayerInfo.soundLevel, 0, 0, 1);
                                return true;

                            } else {
                                updateBackgroundAndFilter(getDrawable(R.drawable.grid_item), movingSquare.firstMatchObject);
                                updateBackgroundAndFilter(getDrawable(R.drawable.grid_item_bad), goalSquare);
                                System.out.println("bad applied to: " + goalSquare.xLocation + goalSquare.yLocation);
                                movingSquare.matches--;
                                //matching sound
                                LevelInfo.soundPool.play(LevelInfo.gridItemStaticFailSoundId, PlayerInfo.soundLevel, PlayerInfo.soundLevel, 0, 0, 1);
                            }
                            break;

                        case 2:

                            if (matchCanBeMadeWith(movingSquare, movingSquare.secondMatchObject, goalSquare, nextMatchNumber)) {
                                updateBackgroundAndFilter(getDrawable(R.drawable.grid_item_good), goalSquare);
                                //references to matching items
                                movingSquare.thirdMatch = goalSquare.getValue();
                                movingSquare.thirdMatchObject = goalSquare;
                                movingSquare.matches++;
                                //matching sound
                                LevelInfo.soundPool.play(LevelInfo.gridItemStaticSoundId, PlayerInfo.soundLevel, PlayerInfo.soundLevel, 0, 0, 1);
                                return true;
                            } else {
                                updateBackgroundAndFilter(getDrawable(R.drawable.grid_item), movingSquare.firstMatchObject);
                                updateBackgroundAndFilter(getDrawable(R.drawable.grid_item), movingSquare.secondMatchObject);
                                updateBackgroundAndFilter(getDrawable(R.drawable.grid_item_bad), goalSquare);
                                movingSquare.matches--;
                                //matching sound
                                LevelInfo.soundPool.play(LevelInfo.gridItemStaticFailSoundId, PlayerInfo.soundLevel, PlayerInfo.soundLevel, 0, 0, 1);
                            }
                            break;

                        case 3:

                            if (matchCanBeMadeWith(movingSquare, movingSquare.thirdMatchObject, goalSquare, nextMatchNumber)) {
                                updateBackgroundAndFilter(getDrawable(R.drawable.grid_item_good), goalSquare);
                                movingSquare.matches++;
                                LevelInfo.soundPool.play(LevelInfo.gridItemStaticSoundId, PlayerInfo.soundLevel, PlayerInfo.soundLevel, 0, 0, 1);
                                return true;
                            } else {
                                updateBackgroundAndFilter(getDrawable(R.drawable.grid_item), movingSquare.firstMatchObject);
                                updateBackgroundAndFilter(getDrawable(R.drawable.grid_item), movingSquare.secondMatchObject);
                                updateBackgroundAndFilter(getDrawable(R.drawable.grid_item), movingSquare.thirdMatchObject);
                                updateBackgroundAndFilter(getDrawable(R.drawable.grid_item_bad), goalSquare);
                                movingSquare.matches--;
                                //matching sound
                                LevelInfo.soundPool.play(LevelInfo.gridItemStaticFailSoundId, PlayerInfo.soundLevel, PlayerInfo.soundLevel, 0, 0, 1);
                            }
                            return true;
                    }

                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    goalSquare = (GridTextView) v;
                    if (goalSquare != movingSquare.firstMatchObject && goalSquare != movingSquare.secondMatchObject && goalSquare != movingSquare.thirdMatchObject) {
                        System.out.println("regular applied to: " + goalSquare.xLocation + goalSquare.yLocation);
                        updateBackgroundAndFilter(getDrawable(R.drawable.grid_item), goalSquare);
                    }
                    return true;

                case DragEvent.ACTION_DROP:

                    goalSquare = (GridTextView) v;
                    goalSquare.setBackground(getDrawable(R.drawable.grid_item));
                    movingSquare.setBackground(getDrawable(R.drawable.grid_item));

                    switch (movingSquare.matches) {

                        case 1:

                            if (matchCanBeMadeWith(movingSquare, movingSquare, goalSquare)) {

                                LevelInfo.currentTurns--;
                                LevelInfo.soundPool.play(LevelInfo.matchMadeSoundId, PlayerInfo.soundLevel, PlayerInfo.soundLevel, 0, 0, 1);
                                //total value for element and score
                                //add sequence total to score
                                if (PlayerInfo.level == 17) {
                                    //total value for element and score
                                    int sequence = movingSquare.getValue() - movingSquare.firstMatch;
                                    String sequenceString = sequence + "";
                                    //change value displayed to be the total value of all matches
                                    goalSquare.setText(sequenceString);
                                    LevelInfo.currentScore += sequence;
                                    PlayerInfo.powerCharge += sequence * 4;
                                }
                                else if (PlayerInfo.level == 18) {


                                    if (goalSquare.yLocation > movingSquare.yLocation || goalSquare.xLocation < movingSquare.xLocation) {
                                        int sequence = movingSquare.getValue() - movingSquare.firstMatch;
                                        String sequenceString = sequence + "";
                                        goalSquare.setText(sequenceString);
                                        LevelInfo.currentScore += sequence;
                                        PlayerInfo.powerCharge -= sequence * 4;
                                    }
                                    else{
                                        int sequence = movingSquare.getValue() + movingSquare.firstMatch;
                                        String sequenceString = sequence + "";
                                        goalSquare.setText(sequenceString);
                                        LevelInfo.currentScore += sequence;
                                        PlayerInfo.powerCharge += sequence * 4;
                                    }
                                }
                                else{
                                    //total value for element and score
                                    int sequence = movingSquare.getValue() + movingSquare.firstMatch;
                                    String sequenceString = sequence + "";
                                    //change value displayed to be the total value of all matches
                                    goalSquare.setText(sequenceString);
                                    LevelInfo.currentScore += sequence;
                                    PlayerInfo.powerCharge += sequence * 4;
                                }

                                updateUI();
                                updateGridItemColour(movingSquare);
                                updateGridItemColour(goalSquare);
                            }
                            break;

                        case 2:
                            if (matchCanBeMadeWith(movingSquare, movingSquare.firstMatchObject, goalSquare)) {

                                LevelInfo.currentTurns--;
                                LevelInfo.soundPool.play(LevelInfo.matchMadeSoundId, PlayerInfo.soundLevel, PlayerInfo.soundLevel, 0, 0, 1);
                                movingSquare.firstMatchObject.setBackground(getDrawable(R.drawable.grid_item));
                                //add sequence total to score
                                if (PlayerInfo.level == 17) {
                                    //total value for element and score
                                    int sequence = movingSquare.getValue() - movingSquare.firstMatch - movingSquare.secondMatch;
                                    String sequenceString = sequence + "";
                                    //change value displayed to be the total value of all matches
                                    goalSquare.setText(sequenceString);
                                    LevelInfo.currentScore += sequence;
                                    PlayerInfo.powerCharge += sequence * 4;
                                }
                                else if (PlayerInfo.level == 18) {


                                    if (goalSquare.yLocation > movingSquare.firstMatchObject.yLocation || goalSquare.xLocation < movingSquare.firstMatchObject.xLocation) {
                                        int sequence = movingSquare.getValue() - movingSquare.firstMatch - movingSquare.secondMatch;
                                        String sequenceString = sequence + "";
                                        goalSquare.setText(sequenceString);
                                        LevelInfo.currentScore += sequence;
                                        PlayerInfo.powerCharge -= sequence * 4;
                                    }
                                    else{
                                        int sequence = movingSquare.getValue() + movingSquare.firstMatch + movingSquare.secondMatch;
                                        String sequenceString = sequence + "";
                                        goalSquare.setText(sequenceString);
                                        LevelInfo.currentScore += sequence;
                                        PlayerInfo.powerCharge += sequence * 4;
                                    }
                                }
                                else{
                                    //total value for element and score
                                    int sequence = movingSquare.getValue() + movingSquare.firstMatch + movingSquare.secondMatch;
                                    String sequenceString = sequence + "";
                                    //change value displayed to be the total value of all matches
                                    goalSquare.setText(sequenceString);
                                    LevelInfo.currentScore += sequence;
                                    PlayerInfo.powerCharge += sequence * 4;
                                }

                                updateUI();
                                updateGridItemColour(goalSquare);
                                updateGridItemColour(movingSquare);
                                updateGridItemColour(movingSquare.firstMatchObject);
                            }
                            break;

                        case 3:
                            if (matchCanBeMadeWith(movingSquare, movingSquare.secondMatchObject, goalSquare)) {

                                LevelInfo.currentTurns--;
                                LevelInfo.soundPool.play(LevelInfo.matchMadeSoundId, PlayerInfo.soundLevel, PlayerInfo.soundLevel, 0, 0, 1);
                                movingSquare.firstMatchObject.setBackground(getDrawable(R.drawable.grid_item));
                                movingSquare.secondMatchObject.setBackground(getDrawable(R.drawable.grid_item));
                                //add sequence total to score
                                if (PlayerInfo.level == 17) {
                                    //total value for element and score
                                    int sequence = movingSquare.getValue() - movingSquare.firstMatch - movingSquare.secondMatch - movingSquare.thirdMatch;
                                    String sequenceString = sequence + "";
                                    //change value displayed to be the total value of all matches
                                    goalSquare.setText(sequenceString);
                                    LevelInfo.currentScore += sequence;
                                    PlayerInfo.powerCharge += sequence * 4;
                                }
                                else if (PlayerInfo.level == 18) {


                                    if (goalSquare.yLocation > movingSquare.secondMatchObject.yLocation || goalSquare.xLocation < movingSquare.secondMatchObject.xLocation) {
                                        int sequence = movingSquare.getValue() - movingSquare.firstMatch - movingSquare.secondMatch - movingSquare.thirdMatch;
                                        String sequenceString = sequence + "";
                                        goalSquare.setText(sequenceString);
                                        LevelInfo.currentScore += sequence;
                                        PlayerInfo.powerCharge -= sequence * 4;
                                    }
                                    else{
                                        int sequence = movingSquare.getValue() + movingSquare.firstMatch + movingSquare.secondMatch + movingSquare.thirdMatch;
                                        String sequenceString = sequence + "";
                                        goalSquare.setText(sequenceString);
                                        LevelInfo.currentScore += sequence;
                                        PlayerInfo.powerCharge += sequence * 4;
                                    }
                                }
                                else{
                                    //total value for element and score
                                    int sequence = movingSquare.getValue() + movingSquare.firstMatch + movingSquare.secondMatch + movingSquare.thirdMatch;
                                    String sequenceString = sequence + "";
                                    //change value displayed to be the total value of all matches
                                    goalSquare.setText(sequenceString);
                                    LevelInfo.currentScore += sequence;
                                    PlayerInfo.powerCharge += sequence * 4;
                                }
                                    PlayerInfo.achievements.put("noSweat", true);
                                //update UI with changes that have been made
                                updateUI();
                                updateGridItemColour(goalSquare);
                                updateGridItemColour(movingSquare);
                                updateGridItemColour(movingSquare.firstMatchObject);
                                updateGridItemColour(movingSquare.secondMatchObject);

                            }
                            break;
                    }
                            //if charge is full and player hasn't activated power-up
                            if (levelUI.powerChargeBar.getProgress() == 100 && !PlayerInfo.powerActivated) {
                                //switch background
                                Button powerButton = findViewById(R.id.power_charge_button);
                                powerButton.setBackground(getDrawable(R.drawable.charge_button_drawable));
                                //play sound if sound hasn't been played yet
                                if (!LevelInfo.powerSoundPlayed) {
                                    LevelInfo.powerSoundPlayed = true;
                                    LevelInfo.soundPool.play(LevelInfo.powerReadySoundId, (PlayerInfo.soundLevel * 0.8f), (PlayerInfo.soundLevel * 0.8f), 0, 0, 1);
                                }
                            }
                            //if power is active and player has made a match
                            if (PlayerInfo.powerActivated && movingSquare.matches != 0) {
                                //deactivate power up and reduce charge to 0
                                PlayerInfo.powerActivated = false;
                                PlayerInfo.powerCharge = 0;
                                levelUI.powerChargeBar.setProgress(PlayerInfo.powerCharge, true);
                                //switch UI back to normal
                                Button powerButton = findViewById(R.id.power_charge_button);
                                powerButton.setBackground(getDrawable(R.drawable.noncharge_button_drawable));
                                LinearLayout chargeLayout = findViewById(R.id.charge_bar_layout);
                                chargeLayout.setBackground(getDrawable(R.drawable.value_text_background));
                                LevelInfo.powerSoundPlayed = false;
                            }
                            //remove any references to older matches
                            movingSquare.matches = 0;
                            movingSquare.firstMatchObject = null;
                            movingSquare.secondMatchObject = null;
                            movingSquare.thirdMatchObject = null;

                            if(LevelInfo.currentScore <= -50 && PlayerInfo.level == 18){
                                PlayerInfo.achievements.put("amIDoingItRight", true);
                                savePlayerInfo();
                            }


                            if (LevelInfo.currentScore >= 3000 && PlayerInfo.isEndless) {
                                PlayerInfo.achievements.put("unstoppable", true);
                                savePlayerInfo();
                            }
                            winCheck();
                            return true;

                        case DragEvent.ACTION_DRAG_ENDED:

                            goalSquare = (GridTextView) v;
                            goalSquare.setBackground(getDrawable(R.drawable.grid_item));
                            updateGridItemColour(goalSquare);
                            // returns true; the value is ignored.
                            return true;

                        default:

                            break;
                    }

                    return false;
            }
    }


    private void updateUI(){
        //make sure charge doesn't go over 100
        if(PlayerInfo.powerCharge > 100){
            PlayerInfo.powerCharge = 100;
        }
        if(PlayerInfo.powerCharge < 0){
            PlayerInfo.powerCharge = 0;
        }

        levelUI.scoreText.setText(String.format(Locale.getDefault(),"%s %d", getString(R.string.score_text), LevelInfo.currentScore));
        levelUI.powerChargeBar.setProgress(PlayerInfo.powerCharge, true);
        levelUI.powerChargeBar.jumpDrawablesToCurrentState();
        try {
            levelUI.turnsText.setText(String.format(Locale.getDefault(),"%s %d", getString(R.string.turns_text), LevelInfo.currentTurns));
            levelUI.targetText.setText(String.format(Locale.getDefault(),"%s %d", getString(R.string.target_text), LevelInfo.targetScore));
            levelUI.levelText.setText(String.format(Locale.getDefault(), "%s %d", getString(R.string.level_text), PlayerInfo.level + 1));
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    //make colour change class
    private void updateGridItemColour(GridTextView gridItem){
        //blue
        if(gridItem.getValue() <= 12){
            gridItem.redInBackground = (200 - gridItem.getValue() * 15);
            gridItem.greenInBackground = (200 - gridItem.getValue() * 15);
            gridItem.blueInBackground = (gridItem.getValue() * 8 + 180);

            if(gridItem.blueInBackground >= 255){

               gridItem.blueInBackground = 255;
            }
        }
        //purple
        else if(gridItem.getValue() <= 24){
            gridItem.redInBackground = (gridItem.getValue() * 8);
            gridItem.greenInBackground = 50;
            gridItem.blueInBackground = 255;

        }
        //red
        else if(gridItem.getValue() <= 36){
            gridItem.redInBackground = (50 + gridItem.getValue() * 5);
            gridItem.greenInBackground = 0;
            gridItem.blueInBackground = (int) (255 * (1 - (gridItem.getValue() / 36.0)));
        }
        //orange
        else if(gridItem.getValue() <= 48){
            gridItem.redInBackground = 255;
            gridItem.greenInBackground = ((gridItem.getValue() - 36) * 15);
            gridItem.blueInBackground = 0;

            if(gridItem.greenInBackground >= 255){

                gridItem.greenInBackground = 255;
            }
        }
        //yellow -> white
        else{
            gridItem.redInBackground = 255;
            gridItem.greenInBackground = 180;
            gridItem.blueInBackground = gridItem.getValue() / 10;
        }

        gridItem.getBackground().setColorFilter(Color.rgb(gridItem.redInBackground, gridItem.greenInBackground, gridItem.blueInBackground), PorterDuff.Mode.OVERLAY);


    }

    private void achievementCheck(){
        //if the player beats level 10 and the difficulty is on easy
        if ((!PlayerInfo.isEndless) && PlayerInfo.level >= 20) {
            //award the first achievement
            PlayerInfo.achievements.put("master", true);

        }

        if(PlayerInfo.level == 3 && LevelInfo.currentTurns == 3){
            PlayerInfo.achievements.put("halfLife3Confirmed", true);
        }

        if(LevelInfo.currentTurns == 0){
            PlayerInfo.achievements.put("EZ", true);
        }

        savePlayerInfo();
    }

    private void loseCheck(){
        boolean gridLock = gridLockCheck();

        if(LevelInfo.currentTurns < 1 || gridLock){
            LevelInfo.soundPool.play(LevelInfo.loseSoundId, PlayerInfo.soundLevel, PlayerInfo.soundLevel, 0, 0, 1);

            LinearLayout loseScreen = new LinearLayout(this);
            Button retryButton = new Button(this);
            retryButton.setText(R.string.retry);
            retryButton.setTextColor(Color.parseColor("#dddddd"));
            retryButton.setBackground(getDrawable(R.drawable.endgame_button_background));
            retryButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
            final Button nextLevelButton = new Button(this);
            nextLevelButton.setText(R.string.next_level);
            nextLevelButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
            nextLevelButton.setTextColor(Color.parseColor("#dddddd"));
            nextLevelButton.setBackground(getDrawable(R.drawable.endgame_button_background));
            Button mainMenuButton = new Button(this);
            mainMenuButton.setText(R.string.main_menu);
            mainMenuButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
            mainMenuButton.setTextColor(Color.parseColor("#dddddd"));
            mainMenuButton.setBackground(getDrawable(R.drawable.endgame_button_background));
            TextView loseMessage = new TextView(this);
            loseMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
            loseMessage.setGravity(Gravity.CENTER);
            loseMessage.setTextColor(Color.parseColor("#dddddd"));

            retryButton.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {

                    LevelInfo.soundPool.release();
                    LevelInfo.soundPool = null;
                    Intent intent = new Intent(v.getContext(), GameScreen.class);
                    startActivity(intent);
                    finish();
                }
            });

            nextLevelButton.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    LevelInfo.soundPool.release();
                    LevelInfo.soundPool = null;
                    savePlayerInfo();
                    PlayerInfo.levelPassed = false;
                    Intent intent = new Intent(v.getContext(), GameScreen.class);
                    startActivity(intent);
                    finish();
                }
            });

            mainMenuButton.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {

                    Intent intent = new Intent(v.getContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            });

            if (gridLock) {
                if (!PlayerInfo.levelPassed && !PlayerInfo.isEndless) {
                    loseMessage.setText(R.string.lf_gridlock);
                }
                else{
                    loseMessage.setText(R.string.gridlock);
                }
                //else loss is because of no turns left
            } else {
                loseMessage.setText(R.string.lf_no_more_turns);
            }

            loseScreen.setOrientation(LinearLayout.VERTICAL);
            loseScreen.addView(loseMessage);
            if (!PlayerInfo.levelPassed) {
                loseScreen.addView(retryButton);
            }
            else if (PlayerInfo.levelPassed && PlayerInfo.level < LevelInfo.levelArray.length()-1){
                loseScreen.addView(nextLevelButton);
            }
            float density = this.getResources().getDisplayMetrics().density;
            int layoutMargin = (int) (50 * density + 0.5f);
            loseScreen.addView(mainMenuButton);
            LinearLayout.LayoutParams paramsRetryButton = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            paramsRetryButton.bottomMargin = layoutMargin;
            retryButton.setLayoutParams(paramsRetryButton);
            LinearLayout.LayoutParams paramsLoseMessage = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            paramsLoseMessage.bottomMargin = layoutMargin;
            loseMessage.setLayoutParams(paramsLoseMessage);
            LinearLayout.LayoutParams paramsNextLevelButton = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            paramsNextLevelButton.bottomMargin = layoutMargin;
            nextLevelButton.setLayoutParams(paramsLoseMessage);
            loseScreen.setBackground(getDrawable(R.drawable.status_text_background));
            loseScreen.setId(View.generateViewId());
            loseScreen.setClickable(true);
            ConstraintLayout constraintLayout = findViewById(R.id.game_constraint_layout);
            constraintLayout.addView(loseScreen);
            ConstraintSet cs = new ConstraintSet();
            cs.clone(constraintLayout);
            cs.connect(loseScreen.getId(), ConstraintSet.TOP, R.id.info_ui_layout, ConstraintSet.TOP, 0);
            cs.applyTo(constraintLayout);
            loseScreen.setElevation(100);
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            loseScreen.setLayoutParams(params);
            loseScreen.setGravity(Gravity.END | Gravity.CENTER);
        }
    }

    private void winCheck(){

        if (LevelInfo.currentScore >= LevelInfo.targetScore && (PlayerInfo.level != 14) && (PlayerInfo.level != 17) || (PlayerInfo.level == 14 && LevelInfo.currentScore <= LevelInfo.targetScore) || (PlayerInfo.level == 17 && LevelInfo.currentScore <= LevelInfo.targetScore)){
            LevelInfo.soundPool.play(LevelInfo.winSoundId, (PlayerInfo.soundLevel * 0.7f), (PlayerInfo.soundLevel * 0.7f), 0, 0, 1);
            final LinearLayout winScreen = new LinearLayout(this);
            Button mainMenuButton = new Button(this);
            mainMenuButton.setText(R.string.main_menu);
            mainMenuButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
            mainMenuButton.setTextColor(Color.parseColor("#dddddd"));
            mainMenuButton.setBackground(getDrawable(R.drawable.endgame_button_background));
            Button closeButton = new Button(this);
            closeButton.setText("");
            closeButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
            closeButton.setGravity(Gravity.CENTER);
            closeButton.setTextColor(Color.parseColor("#dddddd"));
            closeButton.setBackground(getDrawable(R.drawable.close_drawable));
            TextView winMessage = new TextView(this);
            winMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
            winMessage.setTextColor(Color.parseColor("#dddddd"));
            winMessage.setGravity(Gravity.CENTER);
            final Button nextLevelButton = new Button(this);
            nextLevelButton.setText(R.string.next_level);
            nextLevelButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
            nextLevelButton.setTextColor(Color.parseColor("#dddddd"));
            nextLevelButton.setBackground(getDrawable(R.drawable.endgame_button_background));

            mainMenuButton.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    LevelInfo.soundPool.release();
                    LevelInfo.soundPool = null;

                    Intent intent = new Intent(v.getContext(), MainActivity.class);
                    startActivity(intent);
                    finish();

                }
            });

            closeButton.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    PlayerInfo.levelPassed = true;
                    LevelInfo.targetScore = 99999;
                    LevelInfo.currentTurns = 99999;
//                    TextView turnsText = findViewById(R.id.turns_text);
//                    TextView scoreText = findViewById(R.id.score_text);
//                    TextView levelText = findViewById(R.id.level_text);
//                    TextView targetText = findViewById(R.id.target_text);
                    LinearLayout row1 = (LinearLayout) levelUI.turnsText.getParent();
                    LinearLayout row2 = (LinearLayout) levelUI.scoreText.getParent();
                    row1.removeView(levelUI.turnsText);
                    row1.removeView(levelUI.levelText);
                    row2.removeView(levelUI.targetText);

                    ((ViewGroup) winScreen.getParent()).removeView(winScreen);

                    LinearLayout nextLevelParent = (LinearLayout) nextLevelButton.getParent();
                    if(PlayerInfo.level < LevelInfo.levelArray.length()-1) {
                        nextLevelParent.removeView(nextLevelButton);
                        float density = v.getContext().getResources().getDisplayMetrics().density;
                        int layoutMargin = (int) (10 * density + 0.5f);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                        params.bottomMargin = layoutMargin;
                        nextLevelButton.setLayoutParams(params);
                        nextLevelButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                        row1.addView(nextLevelButton);


                    }

                }
            });

            nextLevelButton.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    LevelInfo.soundPool.release();
                    LevelInfo.soundPool = null;
                    savePlayerInfo();
                    PlayerInfo.levelPassed = false;
                    Intent intent = new Intent(v.getContext(), GameScreen.class);
                    startActivity(intent);
                    finish();
                }
            });

            if(PlayerInfo.level < LevelInfo.levelArray.length()-1){

                if(!PlayerInfo.isEndless){

                    if(PlayerInfo.levelHighest < PlayerInfo.level){

                        PlayerInfo.levelHighest = PlayerInfo.level;
                    }
                }
                else {
                    PlayerInfo.level = 0;
                }

//                nextLevelButton.setOnClickListener(new View.OnClickListener() {
//
//                    public void onClick(View v) {
//                        LevelInfo.soundPool.release();
//                        LevelInfo.soundPool = null;
//                        savePlayerInfo();
//                        Intent intent = new Intent(v.getContext(), GameScreen.class);
//                        startActivity(intent);
//                        finish();
//                    }
//                });
                winMessage.setText(String.format(Locale.getDefault(), "%s %d %s", getString(R.string.level_text), (PlayerInfo.level+1), " complete!"));
            }
            else{
                winMessage.setText(R.string.finish);
            }

            if(PlayerInfo.level < LevelInfo.levelArray.length()-1){
                winScreen.setOrientation(LinearLayout.VERTICAL);
                winScreen.addView(closeButton);
                winScreen.addView(winMessage);
                winScreen.addView(nextLevelButton);
                winScreen.addView(mainMenuButton);
                PlayerInfo.level++;
            }
            else{
                winScreen.setOrientation(LinearLayout.VERTICAL);
                winScreen.addView(closeButton);
                winScreen.addView(winMessage);
                winScreen.addView(mainMenuButton);
            }

            winScreen.setBackground(getDrawable(R.drawable.status_text_background));
            winScreen.setClickable(true);
            winScreen.setId(View.generateViewId());
            ConstraintLayout constraintLayout = findViewById(R.id.game_constraint_layout);
            constraintLayout.addView(winScreen);
            winScreen.setElevation(100);

            float density = this.getResources().getDisplayMetrics().density;
            int layoutMargin = (int) (50 * density + 0.5f);
            ConstraintSet cs = new ConstraintSet();
            cs.clone(constraintLayout);
            cs.connect(winScreen.getId(), ConstraintSet.TOP, R.id.info_ui_layout, ConstraintSet.TOP, 0);
            cs.applyTo(constraintLayout);
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            LinearLayout.LayoutParams paramsCloseButton = new LinearLayout.LayoutParams(layoutMargin, layoutMargin);
            LinearLayout.LayoutParams paramsNextLevelButton = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            LinearLayout.LayoutParams paramsMainMenuButton = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            LinearLayout.LayoutParams paramsWinMessage = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            paramsWinMessage.bottomMargin = layoutMargin;
            paramsNextLevelButton.bottomMargin = layoutMargin;
            mainMenuButton.setLayoutParams(paramsMainMenuButton);
            winMessage.setLayoutParams(paramsWinMessage);
            nextLevelButton.setLayoutParams(paramsNextLevelButton);

            winScreen.setLayoutParams(params);
            winScreen.setGravity(Gravity.END | Gravity.CENTER);
            closeButton.setLayoutParams(paramsCloseButton);

            achievementCheck();
        }
        else{
            loseCheck();
        }
    }

    private void savePlayerInfo(){
        FileOutputStream out;
        File file = new File(this.getFilesDir(), "/" + "player_info");
        StringBuilder achievements = new StringBuilder();
        try {
            out = new FileOutputStream(file, false);
            for(int i = 0; i < PlayerInfo.achievements.size(); i++){
                achievements.append(",");
                achievements.append(PlayerInfo.achievements.values().toArray()[i]);
            }
            String fileContents = PlayerInfo.level + "," +
                    PlayerInfo.levelHighest +
                    achievements;

            byte[] contents = fileContents.getBytes();
            out.write(contents);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean gridLockCheck() {

        if(PlayerInfo.powerCharge == 100){
            return false;
        }

        //start as true, if a single move can be made, assign false
        boolean gridLock = true;

        //assign blocks an array of all of the boards pieces
        GridTextView[] blocks = new GridTextView[LevelInfo.gridSize*LevelInfo.gridSize];
        int blockCounter = 0;

        for(int i = 0; i < LevelInfo.gridSize; i++){
            for(int j = 0; j < LevelInfo.gridSize; j++) {

                LinearLayout row = (LinearLayout)levelUI.gameLayout.getChildAt(i);
                blocks[blockCounter] = (GridTextView) row.getChildAt(j);
                blockCounter++;
            }
        }
        //for each piece
        for (GridTextView block1 : blocks) {

            //for every other piece
            for (GridTextView block : blocks) {

                //if a match can be made
                if (Math.abs(block1.getValue() - block.getValue()) == 1 &&
                        ((block1.xLocation == block.xLocation && Math.abs(block1.yLocation - block.yLocation) == 1) ||
                                (block1.yLocation == block.yLocation && Math.abs(block1.xLocation - block.xLocation) == 1))) {
                    //then the grid is not locked
                    gridLock = false;
                }
            }
        }
        //return whether grid is locked
        return gridLock;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        Intent intent;

        switch (id){

            case R.id.action_restart:
                intent = new Intent(this, GameScreen.class);
                startActivity(intent);
                finish();
                return true;

            case R.id.action_main_menu:
                intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void executeOrder66(View view){

        if(Player.PlayerInfo.powerActivated && levelUI.powerChargeBar.getProgress() == 100){
            LevelInfo.soundPool.play(LevelInfo.powerDeactivatedSoundId, (PlayerInfo.soundLevel * 0.7f), (PlayerInfo.soundLevel * 0.8f), 0, 0, 1);
            Player.PlayerInfo.powerActivated = false;
            Snackbar powerSB = Snackbar.make(view, "Power Deactivated", Snackbar.LENGTH_SHORT);
            View v = powerSB.getView();
            TextView txtv = v.findViewById(android.support.design.R.id.snackbar_text);
            txtv.setGravity(Gravity.CENTER_HORIZONTAL);
            txtv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            powerSB.show();
            view.setBackground(getDrawable(R.drawable.value_text_background));
        }

        else if(levelUI.powerChargeBar.getProgress() == 100) {
            LevelInfo.soundPool.play(LevelInfo.powerActivatedSoundId, (PlayerInfo.soundLevel * 0.7f), (PlayerInfo.soundLevel * 0.8f), 0, 0, 1);
            Player.PlayerInfo.powerActivated = true;
            Snackbar powerSB = Snackbar.make(view, "Power Activated", Snackbar.LENGTH_SHORT);
            View v = powerSB.getView();
            TextView txtv = v.findViewById(android.support.design.R.id.snackbar_text);
            txtv.setGravity(Gravity.CENTER_HORIZONTAL);
            txtv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            powerSB.show();
            view.setBackground(getDrawable(R.drawable.layout_charge_active));
        }
    }

    public void muteSound(View view){

        Button soundButton = findViewById(R.id.sound_button);

        System.out.println("test");

        if(Player.PlayerInfo.soundLevel == 1) {
            System.out.println("test 1");
            Player.PlayerInfo.soundLevel = 0;
            soundButton.setBackground(getDrawable(R.drawable.sound_muted));
        }
        else{
            System.out.println("test 2");
            Player.PlayerInfo.soundLevel = 1;
            soundButton.setBackground(getDrawable(R.drawable.sound));
        }
    }

    JSONObject getLevels(){

        String json = null;

        try {
            InputStream is = getAssets().open("levels.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            int bytesRead = is.read(buffer);
            System.out.println(bytesRead);
            is.close();
            json = new String(buffer, "UTF-8");

        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONObject jsonObject = null;
        JSONObject levelJSON = null;

        try {
            jsonObject = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (jsonObject != null) {
            try {
                if(!Player.PlayerInfo.isEndless) {
                    LevelInfo.levelArray = jsonObject.getJSONArray("challenge");
                }
                else{
                    LevelInfo.levelArray = jsonObject.getJSONArray("endless");
                }

                assert LevelInfo.levelArray != null;
                levelJSON = LevelInfo.levelArray.getJSONObject(Player.PlayerInfo.level);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return levelJSON;
    }

    public void showTip(View view){

        Player.PlayerInfo.wantsTip = true;

        if(Player.PlayerInfo.level < 4) {

            LinearLayout powerBar =  (LinearLayout) levelUI.powerChargeBar.getParent();
            levelUI.gameLayout.removeView(powerBar);
            final LinearLayout tipScreen = new LinearLayout(this);
            LinearLayout okBar = new LinearLayout(this);
            okBar.setOrientation(LinearLayout.HORIZONTAL);
            tipScreen.setElevation(100);
            CheckBox tipCB = new CheckBox(this);
            TextView CBText = new TextView(this);
            CBText.setText(getString(R.string.dont_show_message));
            TextView tipText = new TextView(this);
            Button closeButton = new Button(this);
            closeButton.setText("");
            closeButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            closeButton.setGravity(Gravity.END);
            closeButton.setTextColor(Color.parseColor("#dddddd"));
            int states[][] = {{android.R.attr.state_checked}, {}};
            int colors[] = {Color.parseColor("#dddddd"), Color.parseColor("#dddddd")};
            CompoundButtonCompat.setButtonTintList(tipCB, new ColorStateList(states, colors));
            CBText.setTextColor(Color.parseColor("#dddddd"));
            closeButton.setBackground(getDrawable(R.drawable.close_drawable));
            Button okButton = new Button(this);
            okButton.setText(getString(R.string.ok_button));
            okButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            okButton.setGravity(Gravity.CENTER);
            CBText.setGravity(Gravity.CENTER);
            tipCB.setGravity(Gravity.CENTER);
            okButton.setTextColor(Color.parseColor("#dddddd"));
            okButton.setBackground(getDrawable(R.drawable.value_text_background));
            LinearLayout tipLL = new LinearLayout(this);
            tipLL.setOrientation(LinearLayout.VERTICAL);
            tipLL.addView(tipText);
            tipLL.addView(okButton);
            tipLL.addView(okBar);
            tipScreen.addView(tipLL);
            tipText.setTextSize(TypedValue.COMPLEX_UNIT_SP,17);
            tipText.setTextColor(Color.parseColor("#dddddd"));
            tipText.setGravity(Gravity.CENTER);
            okBar.addView(tipCB);
            okBar.addView(CBText);

            tipCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Player.PlayerInfo.wantsTip = !Player.PlayerInfo.wantsTip;
                }
            });

            if(Player.PlayerInfo.wantsTip) {

                tipScreen.setBackground(getDrawable(R.drawable.tip_background));
                tipScreen.setClickable(true);
                tipScreen.setId(View.generateViewId());
                ConstraintLayout constraintLayout = findViewById(R.id.game_constraint_layout);
                constraintLayout.addView(tipScreen);

                okButton.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View v) {
                        ((ViewGroup) tipScreen.getParent()).removeView(tipScreen);
                    }
                });

                ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                float density = this.getResources().getDisplayMetrics().density;
                int tipMargin = (int) (9 * density + 0.5f);
                params.setMargins(tipMargin, tipMargin, tipMargin, tipMargin);
                tipScreen.setLayoutParams(params);
                ConstraintSet cs = new ConstraintSet();
                cs.clone(constraintLayout);
                cs.connect(tipScreen.getId(), ConstraintSet.BOTTOM, levelUI.gameLayout.getId(), ConstraintSet.TOP, tipMargin);
                cs.applyTo(constraintLayout);

                switch (Player.PlayerInfo.level) {
                    case 0:
                        tipText.setText(getString(R.string.tip_one));
                        break;

                    case 1:
                        tipText.setText(getString(R.string.tip_two));
                        break;

                    case 2:
                        tipText.setText(getString(R.string.tip_three));
                        break;

                    case 3:
                        tipText.setText(getString(R.string.tip_four));
                        break;
                }
            }
        }
        else{
            Snackbar tipSB = Snackbar.make(view, "No tip available", Snackbar.LENGTH_SHORT);
            View v = tipSB.getView();
            TextView txtv = v.findViewById(android.support.design.R.id.snackbar_text);
            txtv.setGravity(Gravity.CENTER_HORIZONTAL);
            txtv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tipSB.show();
        }
    }

    boolean matchCanBeMadeWith(GridTextView movingItem, GridTextView previousGoalItem, GridTextView goalItem){
        return (Math.abs(goalItem.getValue() - previousGoalItem.getValue()) == 1 || (PlayerInfo.powerActivated && PlayerInfo.power == OVERRULE)) &&
                ((movingItem.xLocation == goalItem.xLocation && Math.abs(movingItem.yLocation - goalItem.yLocation) == movingItem.matches) ||
                        (movingItem.yLocation == goalItem.yLocation && Math.abs(movingItem.xLocation - goalItem.xLocation) == movingItem.matches));
    }

    boolean matchCanBeMadeWith(GridTextView movingItem, GridTextView previousGoalItem, GridTextView goalItem, int matchNumber){
        return (Math.abs(goalItem.getValue() - previousGoalItem.getValue()) == 1 || (PlayerInfo.powerActivated && PlayerInfo.power == OVERRULE)) &&
                ((movingItem.xLocation == goalItem.xLocation && Math.abs(movingItem.yLocation - goalItem.yLocation) == matchNumber) ||
                        (movingItem.yLocation == goalItem.yLocation && Math.abs(movingItem.xLocation - goalItem.xLocation) == matchNumber));
    }

    void updateBackgroundAndFilter(Drawable background, GridTextView gridItem){
        gridItem.setBackground(background);
        gridItem.getBackground().setColorFilter(Color.rgb(gridItem.redInBackground, gridItem.greenInBackground, gridItem.blueInBackground), PorterDuff.Mode.OVERLAY);
    }

}