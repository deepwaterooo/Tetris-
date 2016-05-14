package dev.ttetris.model;

import dev.ttetris.StarGLSurfaceView;
import dev.ttetris.util.MatrixState;
import dev.ttetris.util.Shader;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.opengl.GLES20;
import android.opengl.Matrix;

public class Cube implements Cloneable, Comparable<Cube>, Serializable {
    //private static final long serialVersionUID = 6144113039836213006L;
	public static float[] mVMatrix = new float[16];
	public static float[] mProjMatrix = new float[16];
	public static float[] mMVPMatrix = new float[16];
    public static float[] mMMatrix = new float[16]; // 具体物体的移动旋转矩阵，旋转、平移
    private static final int COORDS_PER_VERTEX = 3;
    private static final int VALUES_PER_COLOR = 4;
    private final int VERTEX_STRIDE = COORDS_PER_VERTEX * 4;
    private final int COLOR_STRIDE = VALUES_PER_COLOR * 4;
    private ShortBuffer drawListBuffer;
	int mProgram;
	int mMVPMatrixHandle;
	int mPositionHandle;
	int mColorHandle;
	String mVertexShader;
	String mFragmentShader;
	FloatBuffer mVertexBuffer;
	FloatBuffer mColorBuffer;
    private final float size = 0.5f; 
    public float xAngle = 0f;
    private static final float cubeColor[] = {0.0f, 0.0f, 0.0f, 1.0f}; // supposed to change
    private static final short drawOrder[] = {0, 1, 2, 3, 0, 4, 5, 1,  1, 2, 6, 5, 5, 6, 7, 4,  7, 6, 2, 3, 3, 7, 4, 0};
    private CubeColor color; 
    public float [] coords;
    private float x;
    private float y;
    private float z;
    public CubeColor getColor() { return this.color; }
    public void setX(float paramFloat) { this.x = paramFloat; }
    public void setY(float paramFloat) { this.y = paramFloat; }
    public void setZ(float paramFloat) { this.z = paramFloat; }
    public float getX() { return this.x; }
    public float getY() { return this.y; }
    public float getZ() { return this.z; }

    public Cube(StarGLSurfaceView mv, CubeColor paramCubeColor, int paramInt1, int paramInt2, int paramInt3) {
        this.color = paramCubeColor;
        this.x = paramInt1;
        this.y = paramInt2;
        this.z = paramInt3;
        coords = new float[24];
        setCoordinates();
        initShader(mv);
    }

    private boolean isActiveFlag; // for activeBlock
    public void setActiveFlag(boolean v) { this.isActiveFlag = v; }
    public boolean getActiveFlag() { return this.isActiveFlag; }

	public void drawSelf() { // isActiveFlag -- activeBlock; Model.isFrameXRotating
        initVertexData();
		GLES20.glUseProgram(mProgram);

		Matrix.setRotateM(mMMatrix, 0, 0, 0, 1, 0);           // 初始化变换矩阵
        if (getActiveFlag()) {
            //Matrix.rotateM(mMMatrix, 0, xAngle, 0, 0, 1);        // rotate around the center
            if (Model.isFrameZRotating[0]) {       // anti-
                Matrix.rotateM(mMMatrix, 0, xAngle, 0, 0, 1);
            } else if (Model.isFrameZRotating[1]) { // clock-wise
                Matrix.rotateM(mMMatrix, 0, -xAngle, 0, 0, 1);
            } else if (Model.isFrameXRotating[0]) {                // minor bug here for X rotating
                Matrix.translateM(mMMatrix, 0, 0, -2.5f, -5f);
                Matrix.rotateM(mMMatrix, 0, xAngle, 1, 0, 0);
                Matrix.translateM(mMMatrix, 0, 0, 2.5f, 5f);
            } else if (Model.isFrameXRotating[1]) {
                Matrix.translateM(mMMatrix, 0, 0, -2.5f, -5f);
                Matrix.rotateM(mMMatrix, 0, -xAngle, 1, 0, 0);
                Matrix.translateM(mMMatrix, 0, 0, 2.5f, 5f);
            }
            Matrix.translateM(mMMatrix, 0, 2f, 2f, 0.5f);
        } else 
            Matrix.translateM(mMMatrix, 0, 0.5f, 0.5f, 0.5f);    

        Matrix.translateM(mMMatrix, 0, -0.5f, -0.5f, -0.5f); 
        //Matrix.rotateM(mMMatrix, 0, xAngle, 0, 0, 1);        
        if (Model.isFrameZRotating[0]) {       // anti-
            Matrix.rotateM(mMMatrix, 0, xAngle, 0, 0, 1);
        } else if (Model.isFrameZRotating[1]) { // clock-wise
            Matrix.rotateM(mMMatrix, 0, -xAngle, 0, 0, 1);
        } else if (Model.isFrameXRotating[0]) {
            Matrix.translateM(mMMatrix, 0, 0, -2.5f, -5f);
            Matrix.rotateM(mMMatrix, 0, xAngle, 1, 0, 0);
            Matrix.translateM(mMMatrix, 0, 0, 2.5f, 5f);
        } else if (Model.isFrameXRotating[1]) {
            Matrix.translateM(mMMatrix, 0, 0, -2.5f, -5f);
            Matrix.rotateM(mMMatrix, 0, -xAngle, 1, 0, 0);
            Matrix.translateM(mMMatrix, 0, 0, 2.5f, 5f);
        }
        Matrix.translateM(mMMatrix, 0, 0.5f, 0.5f, 0.5f);    

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, Cube.getFinalMatrix(mMMatrix), 0);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE, mVertexBuffer);
        GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 4, mColorBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mColorHandle);
        GLES20.glLineWidth(3.0f);
        GLES20.glDrawElements(GLES20.GL_LINE_LOOP, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
	}
    
    public Cube(CubeColor paramCubeColor, int paramInt1, int paramInt2, int paramInt3) {
        this.color = paramCubeColor;
        this.x = paramInt1;
        this.y = paramInt2;
        this.z = paramInt3;
        coords = new float[24];
        setCoordinates();
    }
    
    public Cube clone() {
        try {
            Cube localCube = (Cube)super.clone();
            return localCube;
        } catch (CloneNotSupportedException localCloneNotSupportedException) {
        }
        return null;
    }

    public int compareTo(Cube paramCube) {
        return Math.abs(this.x - paramCube.x) < 0.00000001f ? 1 : 0;
    }    

    public void setCoordinates() { 
        float [] res = {
            x-size, y+size, z-size, // 0 
            x+size, y+size, z-size, // 1
            x+size, y+size, z+size, // 2
            x-size, y+size, z+size, // 3
            x-size, y-size, z-size, // 4
            x+size, y-size, z-size, // 5
            x+size, y-size, z+size, // 6
            x-size, y-size, z+size  // 7
        };
        coords = res;
    }

    public void initVertexData() {
        ByteBuffer vbb = ByteBuffer.allocateDirect(coords.length*4);
		vbb.order(ByteOrder.nativeOrder());
		mVertexBuffer = vbb.asFloatBuffer();
		mVertexBuffer.put(coords);
		mVertexBuffer.position(0);
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);
		ByteBuffer cbb = ByteBuffer.allocateDirect(cubeColor.length*4);
		cbb.order(ByteOrder.nativeOrder());
		mColorBuffer = cbb.asFloatBuffer();
		mColorBuffer.put(cubeColor);
		mColorBuffer.position(0);
    }

    public void initShader(StarGLSurfaceView mv) {                                // should I set it to be static ?
		mVertexShader = Shader.loadFromAssetsFile("vertex.sh", mv.getResources());
		mFragmentShader = Shader.loadFromAssetsFile("frag.sh", mv.getResources());		
		mProgram = Shader.createProgram(mVertexShader, mFragmentShader);
		mPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
		mColorHandle = GLES20.glGetAttribLocation(mProgram, "aColor");
		mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
	}

	public static float[] getFinalMatrix(float[] spec) {
		mMVPMatrix = new float[16];
		Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, spec, 0);
		Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);
		return mMVPMatrix;
	}
}
