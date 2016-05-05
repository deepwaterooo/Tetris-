package dev.ttetris;

import dev.ttetris.model.Model;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Rect;  
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageButton;
import android.view.Window;
import android.view.WindowManager;

public class ActivityGame extends Activity {
    private StarGLSurfaceView view;
    //private Model model = new Model(7, 20, 7);
    private Model model = new Model();
    private boolean flag = false;
    private int counter = 0;
    private int score;
	Paint paint = null;
    
	private static final String ICICLE_TAG = "simple-tetris";
	private static final String PREFS_HIGH_SCORES = "high_scores";
    private static final String DEBUG_TAG = "Gestures";
    private static float mx;
    private static float my;
    private static float mBgnX;
    private static float mBgnY;
    private static final int SWIPE_MIN_DISTANCE = 5;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE); // for full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        view = new StarGLSurfaceView(this);
        setContentView(view); // ?

		view.setModel(model);
		view.setActivity(this);
        
		// Restore the state:
		if (null != savedInstanceState) 
			onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onPause() {
		super.onPause();
		view.pauseGame();
	}
}