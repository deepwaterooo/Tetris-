package dev.ttetris;

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

public class ActivityGame extends Activity {
    private StarSurfaceView view = null;
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
        view = new StarSurfaceView(this);
        setContentView(view); // ?
        //view.setRenderer(new StarRenderer(this));
		view.setModel(model);
		view.setActivity(this);
        
		// Restore the state:
		if (null != savedInstanceState) {
			onRestoreInstanceState(savedInstanceState);
		}
	}

    public void doMove(Model.Move move) {
		if (model.isGameActive()) {
			view.setGameCommand(move);
			//scoresView.invalidate(); // how to update this one?
		}
	}
    /*  
	public final void startNewGame() {
		if (!model.isGameActive()) {
			//scoresCounter.reset();
            score = 0;
			model.gameStart();
			view.setGameCommandWithDelay(Model.Move.DOWN);
		}
	}
*/
	public void endGame() {
		//messageView.setVisibility(View.VISIBLE);
		//storeHighScoresAndLines();
		//messageView.setText(getApplicationContext().getText(R.string.mode_over));
	}

	public void pauseGame() {
		model.setGamePaused();
		//messageView.setVisibility(View.VISIBLE);
		//messageView.setText(getApplicationContext().getText(R.string.mode_pause));
		//storeHighScoresAndLines();
	}
	
	@Override
	public void onBackPressed() {
		if( model.isGameOver() || model.isGameBeforeStart() || model.isGamePaused() ) {
			finish();
			return;
		}
		if( model.isGameActive() ) {
			pauseGame();
			return;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		pauseGame();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Bundle bundle = new Bundle();
		model.storeTo(bundle);
		//scoresCounter.storeTo(bundle);   // how to save this one?
		outState.putBundle(ICICLE_TAG, bundle);
	}

	@Override
	protected void onRestoreInstanceState(Bundle inState) {
		super.onSaveInstanceState(inState);
		Bundle bundle = inState.getBundle(ICICLE_TAG);
		if (null != bundle) {
			model.restoreFrom(bundle);
			//scoresCounter.restoreFrom(bundle);  // how to restoret this one?
		}
		pauseGame();
	}
}
