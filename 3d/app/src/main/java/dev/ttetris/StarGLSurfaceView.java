package dev.ttetris;

import dev.ttetris.model.Cube;
import dev.ttetris.model.Block;
import dev.ttetris.model.Model;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.os.Handler;
import android.util.AttributeSet;
import android.os.Bundle;
import javax.microedition.khronos.opengles.GL10;

import dev.ttetris.model.BlockType;
import dev.ttetris.model.CubeColor;
import dev.ttetris.model.Cube;
import dev.ttetris.model.Block;
import dev.ttetris.model.Model;
import dev.ttetris.util.MatrixState;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.util.Log;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import javax.vecmath.Matrix4f;

public class StarGLSurfaceView extends GLSurfaceView {
    final float ANGLE_SPAN = 0.375f;
    Cube cube;
    
    private static StarGLSurfaceView mStarGLSurfaceView = null;
    //private StarRenderer mStarRenderer; // final?
    private StarRenderer mStarRenderer; 

    private static Handler sHandler; // do I need a handler?

	private ActivityGame activity;
    private Model model;
    public int DELAY = 100;
	private long lastMove = 0;
    
    Block activeBlock;
	Block nextBlock;  // keep this variable, but temporatorially NOT drawing them

    // ��¼�ϴδ���λ�õ����� 
    private float mPreviousX, mPreviousY; 
    
    public StarGLSurfaceView(Context context) {
        super(context);
        setDebugFlags(DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS); // Turn on error-checking and logging
        setEGLContextClientVersion(2);
        initView();
    }

    public StarGLSurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setDebugFlags(DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS); 
        setEGLContextClientVersion(2);
        initView();
    }
    /*
      public StarGLSurfaceView(Context context, OnSurfacePickedListener onSurfacePickedListener) {
      super(context, onSurfacePickedListener);
      setDebugFlags(DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS);
      setEGLContextClientVersion(2);
      initView();
      // ͸����һ��View 
      setZOrderOnTop(true); 
      setEGLConfigChooser(8, 8, 8, 8, 16, 0); 
      // ͸����һ��Activity 
      getHolder().setFormat(PixelFormat.TRANSLUCENT);
      mStarRenderer.setOnSurfacePickedListener(onSurfacePickedListener);
      } */
    
    private void initView() {
        //cube = new Cube(this, 1, 1, 3, -6, 3);
        mStarRenderer = new StarRenderer(); //������Ⱦ��
        setRenderer(mStarRenderer);  //������Ⱦ��
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mStarGLSurfaceView = this; // do I really need this one?
        setFocusableInTouchMode(true);
        model = new Model();
    }
    
    public boolean onTouchEvent(final MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        //AppConfig.setTouchPosition(x, y);  // sth I need to code ///////////////////////////////
        switch (e.getAction()) {
        case MotionEvent.ACTION_MOVE:
            // �������ĵ�����Ʒ�����ʱ����ת90�������� 
            float dx = y - mPreviousY; 
            float dy = x - mPreviousX; 
            // ���ƾ��� 
            float d = (float) (Math.sqrt(dx * dx + dy * dy)); 
            // ��ת�ᵥλ������x,yֵ��z=0�� 
            mStarRenderer.mfAngleX = dx; 
            mStarRenderer.mfAngleY = dy; 
            // ���ƾ��� 
            mStarRenderer.gesDistance = d; 
 
            // float dx = x - mPreviousX; 
            // float dy = y - mPreviousY; 
            // mStarRenderer.mfAngleY += dx * TOUCH_SCALE_FACTOR; 
            // mStarRenderer.mfAngleX += dy * TOUCH_SCALE_FACTOR; 
 
            // PickFactory.update(x, y); 
            //AppConfig.gbNeedPick = false; 
            break; 
        case MotionEvent.ACTION_DOWN: 
            //AppConfig.gbNeedPick = false; 
            break; 
        case MotionEvent.ACTION_UP: 
            //AppConfig.gbNeedPick = true; 
            break; 
        case MotionEvent.ACTION_CANCEL: 
            //AppConfig.gbNeedPick = false; 
            break; 
        } 
        mPreviousX = x; 
        mPreviousY = y; 
        return true; 
    }
    
    public void setModel(Model model) {
		this.model = model;
	}

	public void setActivity(ActivityGame activity) {
		this.activity = activity;
	}

	public void endGame() {
	}

	public void pauseGame() {
		//model.setGamePaused();
	}

    public enum BlockColor {  // set in Block
        RED(0xffff0000, (byte) 1),
        GREEN(0xff00ff00, (byte) 2),
        BLUE(0xff0000ff, (byte) 3),
        YELLOW(0xffffff00, (byte) 4),
        CYAN(0xff00ffff, (byte) 5),
        WHITE(0xffffffff, (byte) 6),
        MAGENTA(0xffff00ff, (byte) 7),
        TRANSPARENT(0x20320617, (byte) 8);
        private final int color;
        private final byte value;
        private BlockColor(int color, byte value) {
            this.color = color;
            this.value = value;
        }
    }

    private class StarRenderer implements GLSurfaceView.Renderer {
        private static final String TAG = "StarRenderer";
        private final float[] mMVPMatrix = new float[16];
        private final float[] mProjectionMatrix = new float[16];
        private final float[] mViewMatrix = new float[16];
        private final float[] mRotationMatrix = new float[16];
        private final int unitSize = 1;
        private final float cubeSize = 0.11428f;
        //private final float cubeSize = 0.22857f;   // supposed to set & get later

        private float mAngle;
        public DownThread downThread;
        private Model model = new Model();
        //Cube cube;
    
        private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "attribute vec4 vColor;" +
            "varying vec4 _vColor;" +
            "void main() {" +
            "  _vColor = vColor;" + 
            "  gl_Position = uMVPMatrix * vPosition;" +
            "}";
        private final String fragmentShaderCode =
            "precision mediump float;" +
            "varying vec4 _vColor;" +
            "void main() {" +
            "  gl_FragColor = _vColor;" +
            "}";

        private FloatBuffer vertexBuffer;
        private FloatBuffer mColorBuffer;
        private ShortBuffer drawListBuffer;
        private int mProgram;
        private int mPositionHandle;
        private int mColorHandle;
        private int mMVPMatrixHandle;
        private static final int COORDS_PER_VERTEX = 3;
        private static final int VALUES_PER_COLOR = 4;
        private final int VERTEX_STRIDE = COORDS_PER_VERTEX * 4;
        private final int COLOR_STRIDE = VALUES_PER_COLOR * 4;

        public static final int VERTEX_BUFFER = 0; 
        public static final int TEXTURE_BUFFER = 1; 
        public float mfAngleX = 0.0f; 
        public float mfAngleY = 0.0f; 
        public float gesDistance = 0.0f; 
        //private static float one = 1.0f; e
        private float one = 1.0f; 

        private Block currBlock;
        
        private float color[] = {1.0f, 1.0f, 1.0f, 1.0f};
        private  short drawOrder[] = { // for cubes
            0, 1, 2, 3, 0, 4, 5, 1,
            1, 2, 6, 5, 5, 6, 7, 4,
            7, 6, 2, 3, 3, 7, 4, 0};

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(1.0f, 1.0f, 0.0f, 0.0f); // yellow
            //initShapes();
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
            mProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(mProgram, vertexShader);
            GLES20.glAttachShader(mProgram, fragmentShader);
            GLES20.glLinkProgram(mProgram);
            currBlock = new Block(BlockType.squareType);
            //currBlock = new Block(BlockType.lineType);

            cube = new Cube(StarGLSurfaceView.this, 1, 0.114285f, 0, 0, 0);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_CULL_FACE);
        
            downThread = new DownThread();
            downThread.start();
        }

        public class DownThread extends Thread {
            public boolean flag = true;

            @Override
            public void run() {
                while (flag) {
                    try {
                        Thread.sleep(400);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int w, int h) {
            GLES20.glViewport(0, 0, w, h);           //�����Ӵ�
            float ratio = (float) w / h;
            Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 10); // ͶӰ����

            Constant.ratio = (float) ratio;
            //MatrixState.setProjectFrustum(-Constant.ratio * 0.8f, Constant.ratio * 1.2f, -1, 1, 3, 7);
            MatrixState.setProjectFrustum(-Constant.ratio, Constant.ratio, -1, 1, 3, 10);
            MatrixState.setCamera(4.8f, 2.2f, 4.5f, 0, 0, 0, 0, 1.0f, 0f);   // need to change this one
            MatrixState.setInitStack();
        }
     
        @Override
        public void onDrawFrame(GL10 gl) {
            GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
            //Matrix.setLookAtM(Cube.mVMatrix, 0, 4.2f, 2.2f, 2.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
            Matrix.setLookAtM(mViewMatrix, 0, 4.8f, 2.2f, 4.5f, 0f, 0f, 0f, 0f, 1.0f, 0.0f); // good

            // �ϲ�ͶӰ���ӿھ��� Calculate the projection and view transformation
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0); 
            GLES20.glUseProgram(mProgram);

            drawGameFrame();
            drawCurrBlock();  // should drawboard
            /*
            for (int i = 6; i < Model.ROW; i++) 
                for (int j = 6; j < Model.COL; j++) 
                    model.board[i][j][0] = 1;
            */
            //drawGameCell();


            MatrixState.pushMatrix();
            //drawGameCell();
            cube.drawSelf();
            //drawCurrBlock();
            MatrixState.popMatrix();

            MatrixState.pushMatrix();
            //MatrixState.translate(.22857f, 0f, .22857f);
            //MatrixState.translate(0.45714f, 0f, 0.45714f);
            //MatrixState.translate(0.68571f, -0.8f, 0.571425f);
            //MatrixState.translate(-.342855f, -0.8f, -.342855f);
            //MatrixState.translate(0f, -0.8f, 0f);
            MatrixState.translate(0, -1.14285f, 0);
            //drawCurrBlock();
            cube.drawSelf();
            //drawGameCell();
            MatrixState.popMatrix();

        }

        private float [] getCubeCoordinates(int i, int j, int k) {
            float [] res = {
                i-unitSize, j-unitSize, k-unitSize,
                i+unitSize, j-unitSize, k-unitSize,
                i+unitSize, j+unitSize, k-unitSize,
                i-unitSize, j+unitSize, k-unitSize,
                i-unitSize, j-unitSize, k+unitSize,
                i+unitSize, j-unitSize, k+unitSize,
                i+unitSize, j+unitSize, k+unitSize,
                i-unitSize, j+unitSize, k+unitSize};
            return res;
        }

        private void drawGameCell() { // shifts
            float [] cubeCoords = new float[24];
            for (int k = 0; k < Model.HIG; k++) { // y
                for (int j = 0; j < Model.COL; j++) {  // z
                    for (int i = 0; i < Model.ROW; i++) { // x
                        if (model.board[i][j][k] != 0 && model.board[i][j][k] != 8) {
                            //cubeCoords = getCubeCoordinates(i - 3, k - 7, j - 3);  // matrix bug
                            //cube = new Cube(1, 1, 3, -7, 3);

                            //cubeCoords = getCubeCoordinates(i - 6, k, j - 6);
                            cubeCoords = getCubeCoordinates(3, -7, 3);
                            /*
                              for (int m = 0; m < 24; m++) {
                              cubeCoords[m] *= 0.11428f;
                              //cubeCoords[m] *= 0.22857f;
                              if (m % 3 == 1)
                              cubeCoords[m] -= 0.8f;
                              } */
                            drawCube(cubeCoords);
                        }
                    }
                }
            }
        }
    

        private void drawCube(float [] squareCoords) {
            ByteBuffer vbb = ByteBuffer.allocateDirect(squareCoords.length * 4); 
            vbb.order(ByteOrder.nativeOrder());  
            vertexBuffer = vbb.asFloatBuffer();  
            vertexBuffer.put(squareCoords);      
            vertexBuffer.position(0);            
            ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
            dlb.order(ByteOrder.nativeOrder());
            drawListBuffer = dlb.asShortBuffer();
            drawListBuffer.put(drawOrder);
            drawListBuffer.position(0);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(color.length * 4);
            byteBuffer.order(ByteOrder.nativeOrder());
            mColorBuffer = byteBuffer.asFloatBuffer();
            mColorBuffer.put(color);
            mColorBuffer.position(0);
        
            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer);
            GLES20.glEnableVertexAttribArray(mColorHandle);
            GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 4, mColorBuffer);

            // �����״�ı任�����handle
            mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            //StarRenderer.checkGlError("glGetUniformLocation");

            // Ӧ��ͶӰ���ӿڱ任
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, MatrixState.getFinalMatrix(), 0);
            //GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            //StarRenderer.checkGlError("glUniformMatrix4fv");

            GLES20.glLineWidth(3.0f);
            GLES20.glDrawElements(GLES20.GL_LINE_LOOP, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
            //GLES20.glDisableVertexAttribArray(mPositionHandle);
            //GLES20.glDisableVertexAttribArray(mColorHandle);
        }
    
        public void drawCurrBlock() {
            //currBlock.shiftBlock(0f, -0.11428f, 0f); // this method does NOT work
            Cube [] cubes = currBlock.getCubes();
            for (int j = 0; j < 4; j++) {
                float squareCoords [] = cubes[j].getCubeCoordinates();
                for (int i = 0; i < 24; i++) {
                    squareCoords[i] *= 0.11428f;
                    /*
                    if (i % 3 == 1)
                        //squareCoords[i] += 1.6f;
                        squareCoords[i] += -1.114285f;  // this translation is NOT correct
                    */
                    /*
                    else if (i % 3 == 2)  // for line only
                        squareCoords[i] += 0.11428f;
                    */
                }
                ByteBuffer vbb = ByteBuffer.allocateDirect(squareCoords.length * 4); 
                vbb.order(ByteOrder.nativeOrder());  // use the device hardware's native byte order
                vertexBuffer = vbb.asFloatBuffer();  // create a floating point buffer from the ByteBuffer
                vertexBuffer.put(squareCoords);      // add the coordinates to the FloatBuffer
                vertexBuffer.position(0);            // set the buffer to read the first coordinate
                ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
                dlb.order(ByteOrder.nativeOrder());
                drawListBuffer = dlb.asShortBuffer();
                drawListBuffer.put(drawOrder);
                drawListBuffer.position(0);
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(color.length * 4);
                byteBuffer.order(ByteOrder.nativeOrder());
                mColorBuffer = byteBuffer.asFloatBuffer();
                mColorBuffer.put(color);
                mColorBuffer.position(0);

                //drawLineLoop(); // drawOrder for cubes
                mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
                GLES20.glEnableVertexAttribArray(mPositionHandle);
                GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer);
                GLES20.glEnableVertexAttribArray(mColorHandle);
                GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 4, mColorBuffer);

                mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
                //StarRenderer.checkGlError("glGetUniformLocation");
                GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
                //StarRenderer.checkGlError("glUniformMatrix4fv");

                GLES20.glLineWidth(5.0f);
                GLES20.glDrawElements(GLES20.GL_LINE_LOOP, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
                GLES20.glDisableVertexAttribArray(mPositionHandle);
                GLES20.glDisableVertexAttribArray(mColorHandle);
            } 
        }

        //public short drawOrder0[] = {0, 4, 5, 1, 1, 0, 3, 2, 2, 6, 7, 3, 3, 7, 4, 0//};  // 4 surfaces
        public short drawOrder0[] = {4, 5, 1, 0, 3, 2, 1, 0, 4, 7, 3, 0,   // 3 surfaces
                                     16, 17, 15, 14,
                                     12, 13, 11, 10,
                                     8, 9, 4, 18,
                                     19, 21, 20, 22,
                                     23, 25, 24, 26,
                                     26, 27, 5, 4};
        private void drawGameFrame() {
            //float delta = 1f;
            float squareCoords [] = { 
                -3f, -5f, -3f,
                3f, -5f, -3f,
                3f, 5f, -3f,
                -3f, 5f, -3f, // 3 
                -3f, -5f, 3f,
                3f, -5f, 3f, // 5
                3f, 5f, 3f,
                -3f, 5f, 3f,  // 7
                -2f, -5f, -3f,
                -2f, -5f, 3f,  // 9
                -1f, -5f, -3f,
                -1f, -5f, 3f, // 11
                0f, -5f, -3f,
                0f, -5f, 3f,  // 13
                 1f, -5f, -3f,
                1f, -5f, 3f, // 15
                 2f, -5f, -3f,
                2f, -5f, 3f, // 17
                -3f, -5f, -2f,
                3f, -5f, -2f,  // 19  // one
                -3f, -5f, -1f,
                3f, -5f, -1f, // 21
                -3f, -5f, 0f,
                3f, -5f, 0f, // 23
                -3f, -5f, 1f,
                3f, -5f, 1f, // 25
                -3f, -5f, 2f,
                3f, -5f, 2f // 27
            }; 
           for (int i = 0; i < 84; i++) {
               /*if (i % 3 == 1)
                      squareCoords[i] *= 0.32f;
                      else*/ squareCoords[i] *= (cubeSize * 2);
           }
            ByteBuffer vbb = ByteBuffer.allocateDirect(squareCoords.length * 4); 
            vbb.order(ByteOrder.nativeOrder());  // use the device hardware's native byte order
            vertexBuffer = vbb.asFloatBuffer();  // create a floating point buffer from the ByteBuffer
            vertexBuffer.put(squareCoords);      // add the coordinates to the FloatBuffer
            vertexBuffer.position(0);            // set the buffer to read the first coordinate
            ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder0.length * 2);
            dlb.order(ByteOrder.nativeOrder());
            drawListBuffer = dlb.asShortBuffer();
            drawListBuffer.put(drawOrder0);
            drawListBuffer.position(0);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(color.length * 4);
            byteBuffer.order(ByteOrder.nativeOrder());
            mColorBuffer = byteBuffer.asFloatBuffer();
            mColorBuffer.put(color);
            mColorBuffer.position(0);
            drawLineLoop();
        }

        private void drawLineLoop() { // pass in drawOrder as argument
            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer);
            GLES20.glEnableVertexAttribArray(mColorHandle);
            GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 4, mColorBuffer);
            mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            //StarRenderer.checkGlError("glGetUniformLocation");
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            //StarRenderer.checkGlError("glUniformMatrix4fv");
            GLES20.glLineWidth(5.0f);
            GLES20.glDrawElements(GLES20.GL_LINE_LOOP, drawOrder0.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
            GLES20.glDisableVertexAttribArray(mPositionHandle);
            GLES20.glDisableVertexAttribArray(mColorHandle);
        }
    
        private void initShapes(Cube cube) {
            float squareCoords [] = cube.getCubeCoordinates();
            ByteBuffer vbb = ByteBuffer.allocateDirect(squareCoords.length * 4); 
            vbb.order(ByteOrder.nativeOrder());  // use the device hardware's native byte order
            vertexBuffer = vbb.asFloatBuffer();  // create a floating point buffer from the ByteBuffer
            vertexBuffer.put(squareCoords);      // add the coordinates to the FloatBuffer
            vertexBuffer.position(0);            // set the buffer to read the first coordinate
            ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
            dlb.order(ByteOrder.nativeOrder());
            drawListBuffer = dlb.asShortBuffer();
            drawListBuffer.put(drawOrder);
            drawListBuffer.position(0);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(color.length * 4);
            byteBuffer.order(ByteOrder.nativeOrder());
            mColorBuffer = byteBuffer.asFloatBuffer();
            mColorBuffer.put(color);
            mColorBuffer.position(0);
        }

        public  int loadShader(int type, String shaderCode){
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode); // add the source code to the shader and compile it
            GLES20.glCompileShader(shader);
            return shader;
        }

        public  void checkGlError(String glOperation) {
            int error;
            while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                Log.e(TAG, glOperation + ": glError " + error);
                throw new RuntimeException(glOperation + ": glError " + error);
            }
        }
    
        /*    // �����嶥������ 
              private float[] vertices = new float[] { -one, -one, one, one, -one, one, 
              one, one, one, -one, one, one, -one, -one, -one, -one, one, -one, 
              one, one, -one, one, -one, -one, -one, one, -one, -one, one, one, 
              one, one, one, one, one, -one, -one, -one, -one, one, -one, -one, 
              one, -one, one, -one, -one, one, one, -one, -one, one, one, -one, 
              one, one, one, one, -one, one, -one, -one, -one, -one, -one, one, 
              -one, one, one, -one, one, -one }; 
              // �������������� 
              private float[] texCoords = new float[] { one, 0, 0, 0, 0, one, one, one, 
              0, 0, 0, one, one, one, one, 0, one, one, one, 0, 0, 0, 0, one, 0, 
              one, one, one, one, 0, 0, 0, 0, 0, 0, one, one, one, one, 0, one, 
              0, 0, 0, 0, one, one, one }; 
              // ����������˳�� 
              private byte[] indices = new byte[] { 0, 1, 3, 2, 4, 5, 7, 6, 8, 9, 11, 10, 12, 13, 15, 14, 16, 17, 19, 18, 20, 21, 23, 22 }; 
        */

        // for rotate and detecting
        // ����������ĳһ��ı�ǣ�0��5��
        public int surface = -1;
        /*
        // ��ȡ����Ļ������
        public FloatBuffer getCoordinate(int coord_id) {
        switch (coord_id) {
        case VERTEX_BUFFER:
        return getDirectBuffer(squareCoords);
        //case TEXTURE_BUFFER:
        //return getDirectBuffer();
        default:
        throw new IllegalArgumentException();
        }
        }

        // ��ȡ�����ε�����˳��
        public ByteBuffer getIndices() {
        return ByteBuffer.wrap(drawListBuffer);
        }*/

        public FloatBuffer getDirectBuffer(float [] buffer) {
            ByteBuffer bb = ByteBuffer.allocateDirect(buffer.length * 4);
            bb.order(ByteOrder.nativeOrder());
            FloatBuffer directBuffer = bb.asFloatBuffer();
            directBuffer.put(buffer);
            directBuffer.position(0);
            return directBuffer;
        }

        // ��������������Բ�����ĵ�
        public Vector3f getSphereCenter() {
            return new Vector3f(0, 0, 0);
        }

        // ��������������Բ�İ뾶��sqrt(3))
        public float getSphereRadius() {
            return 1.732051f;
        }

        private Vector3f transformedSphereCenter = new Vector3f(); 
        //private Ray transformedRay = new Ray(); 
        private Matrix4f matInvertModel = new Matrix4f(); 
        private Vector3f[] mpTriangle = {
            new Vector3f(),
            new Vector3f(), 
            new Vector3f()
        };     

        /** 
         * ������ģ�͵ľ�ȷ��ײ��� 
         * @param ray - ת����ģ�Ϳռ��е����� 
         * @param trianglePosOut - ���ص�ʰȡ��������ζ���λ�� 
         * @return ����ཻ������true 

         public boolean intersect(Ray ray, Vector3f [] trianglePosOut) {
         boolean bFound = false;
         // �洢��л�������������ཻ��ľ��룬���������������������һ��
         float closeDis = 0.0f;
         Vector3f v0, v1, v2;

         // �������6����
         for (int i = 0; i < 6; i++) {
         // ÿ����������������
         for (int j = 0; j < 2; j++) {
         if (j == 0) {
         v0 = getVector3f(indices[i * 4 + j]);
         v1 = getVector3f(indices[i * 4 + j + 1]);
         v2 = getVector3f(indices[i * 4 + j + 2]);
         } else {
         // �ڶ���������ʱ������˳�򣬲�Ȼ����Ⱦ���������ڲ�
         v0 = getVector3f(indices[i * 4 + j]);
         v1 = getVector3f(indices[i * 4 + j + 2]);
         v2 = getVector3f(indices[i * 4 + j + 1]);
         }
         // �������ߺ������εĴ�ײ���
         if (ray.intersectTriangle(v0, v1, v2, location)) {
         // ����������ཻ
         if (!bFound) {
         // ����ǳ��μ�⵽����Ҫ�洢����ԭ���������ν���ľ���ֵ
         bFound = true;
         closeDis = location.w;
         trianglePosOut[0].set(v0);
         trianglePosOut[1].set(v1);
         trianglePosOut[2].set(v2);
         surface = i;
         } else {
         // ���֮ǰ�Ѿ���⵽�ཻ�¼�������Ҫ�����ཻ����֮ǰ���ཻ��������Ƚϣ����������ߵ������
         if (closeDis > location.w) {
         closeDis = location.w;
         trianglePosOut[0].set(v0);
         trianglePosOut[1].set(v1);
         trianglePosOut[2].set(v2);
         surface = i;
         }
         }
         }
         }

         }
         return bFound;
         }

         private Vector3f getVector3f(int start) {
         return new Vector3f(vertices[3 * start], vertices[3 * start + 1], vertices[3 * start + 2]);
         }
        */    
    }
    
}