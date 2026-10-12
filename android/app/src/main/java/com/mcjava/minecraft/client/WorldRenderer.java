package com.mcjava.minecraft.client;

import android.opengl.GLES30;
import android.util.Log;

public class WorldRenderer {
    private static final String TAG = "WorldRenderer";
    
    private int width, height;
    private boolean initialized = false;
    
    // Shaders
    private int basicProgram;
    private int blockProgram;
    
    // Buffers
    private int[] vbo = new int[1];
    private int[] vao = new int[1];
    
    public WorldRenderer() {
        Log.i(TAG, "WorldRenderer created");
    }
    
    public void onSurfaceCreated() {
        Log.i(TAG, "Surface created, initializing OpenGL");
        
        // Enable features
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glCullFace(GLES30.GL_BACK);
        GLES30.glFrontFace(GLES30.GL_CCW);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        
        // Clear color (sky blue)
        GLES30.glClearColor(0.53f, 0.81f, 0.92f, 1.0f);
        
        // Initialize shaders
        initShaders();
        
        // Initialize geometry
        initGeometry();
        
        initialized = true;
        Log.i(TAG, "OpenGL initialized");
    }
    
    public void onSurfaceChanged(int width, int height) {
        this.width = width;
        this.height = height;
        GLES30.glViewport(0, 0, width, height);
        Log.i(TAG, "Surface changed: " + width + "x" + height);
    }
    
    public void render() {
        if (!initialized) return;
        
        // Clear
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        
        // TODO: Render world, blocks, entities, etc.
        
        checkGlError("render");
    }
    
    public void dispose() {
        if (initialized) {
            if (basicProgram != 0) {
                GLES30.glDeleteProgram(basicProgram);
            }
            if (blockProgram != 0) {
                GLES30.glDeleteProgram(blockProgram);
            }
            if (vbo[0] != 0) {
                GLES30.glDeleteBuffers(1, vbo, 0);
            }
            if (vao[0] != 0) {
                GLES30.glDeleteVertexArrays(1, vao, 0);
            }
            initialized = false;
        }
    }
    
    private void initShaders() {
        // Basic vertex shader
        String vertexShaderSource = 
            "#version 300 es\n" +
            "layout(location = 0) in vec3 aPosition;\n" +
            "layout(location = 1) in vec2 aTexCoord;\n" +
            "layout(location = 2) in vec3 aNormal;\n" +
            "uniform mat4 uProjection;\n" +
            "uniform mat4 uView;\n" +
            "uniform mat4 uModel;\n" +
            "out vec2 vTexCoord;\n" +
            "out vec3 vNormal;\n" +
            "out vec3 vWorldPos;\n" +
            "void main() {\n" +
            "    vTexCoord = aTexCoord;\n" +
            "    vNormal = mat3(uModel) * aNormal;\n" +
            "    vec4 worldPos = uModel * vec4(aPosition, 1.0);\n" +
            "    vWorldPos = worldPos.xyz;\n" +
            "    gl_Position = uProjection * uView * worldPos;\n" +
            "}";
        
        // Basic fragment shader
        String fragmentShaderSource = 
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "in vec2 vTexCoord;\n" +
            "in vec3 vNormal;\n" +
            "in vec3 vWorldPos;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform vec3 uLightDir;\n" +
            "uniform vec3 uLightColor;\n" +
            "uniform vec3 uAmbientColor;\n" +
            "out vec4 fragColor;\n" +
            "void main() {\n" +
            "    vec3 normal = normalize(vNormal);\n" +
            "    float diff = max(dot(normal, uLightDir), 0.0);\n" +
            "    vec3 diffuse = diff * uLightColor;\n" +
            "    vec3 ambient = uAmbientColor;\n" +
            "    vec4 texColor = texture(uTexture, vTexCoord);\n" +
            "    vec3 result = (ambient + diffuse) * texColor.rgb;\n" +
            "    fragColor = vec4(result, texColor.a);\n" +
            "}";
        
        basicProgram = createProgram(vertexShaderSource, fragmentShaderSource);
        
        // Block shader (simpler, no lighting)
        String blockVertexShader = 
            "#version 300 es\n" +
            "layout(location = 0) in vec3 aPosition;\n" +
            "layout(location = 1) in vec2 aTexCoord;\n" +
            "uniform mat4 uProjection;\n" +
            "uniform mat4 uView;\n" +
            "uniform mat4 uModel;\n" +
            "out vec2 vTexCoord;\n" +
            "void main() {\n" +
            "    vTexCoord = aTexCoord;\n" +
            "    gl_Position = uProjection * uView * uModel * vec4(aPosition, 1.0);\n" +
            "}";
        
        String blockFragmentShader = 
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "in vec2 vTexCoord;\n" +
            "uniform sampler2D uTexture;\n" +
            "out vec4 fragColor;\n" +
            "void main() {\n" +
            "    fragColor = texture(uTexture, vTexCoord);\n" +
            "}";
        
        blockProgram = createProgram(blockVertexShader, blockFragmentShader);
    }
    
    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource);
        
        if (vertexShader == 0 || fragmentShader == 0) {
            return 0;
        }
        
        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);
        
        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0);
        
        if (linkStatus[0] != GLES30.GL_TRUE) {
            Log.e(TAG, "Program link failed: " + GLES30.glGetProgramInfoLog(program));
            GLES30.glDeleteProgram(program);
            return 0;
        }
        
        GLES30.glDeleteShader(vertexShader);
        GLES30.glDeleteShader(fragmentShader);
        
        return program;
    }
    
    private int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);
        
        int[] compileStatus = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0);
        
        if (compileStatus[0] != GLES30.GL_TRUE) {
            Log.e(TAG, "Shader compile failed: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        
        return shader;
    }
    
    private void initGeometry() {
        // Create a simple cube for testing
        float[] vertices = {
            // Positions          // TexCoords  // Normals
            // Front face
            -0.5f, -0.5f,  0.5f,  0.0f, 0.0f,  0.0f, 0.0f, 1.0f,
             0.5f, -0.5f,  0.5f,  1.0f, 0.0f,  0.0f, 0.0f, 1.0f,
             0.5f,  0.5f,  0.5f,  1.0f, 1.0f,  0.0f, 0.0f, 1.0f,
            -0.5f,  0.5f,  0.5f,  0.0f, 1.0f,  0.0f, 0.0f, 1.0f,
            // Back face
            -0.5f, -0.5f, -0.5f,  1.0f, 0.0f,  0.0f, 0.0f, -1.0f,
             0.5f, -0.5f, -0.5f,  0.0f, 0.0f,  0.0f, 0.0f, -1.0f,
             0.5f,  0.5f, -0.5f,  0.0f, 1.0f,  0.0f, 0.0f, -1.0f,
            -0.5f,  0.5f, -0.5f,  1.0f, 1.0f,  0.0f, 0.0f, -1.0f,
            // ... other faces would go here
        };
        
        GLES30.glGenVertexArrays(1, vao, 0);
        GLES30.glGenBuffers(1, vbo, 0);
        
        GLES30.glBindVertexArray(vao[0]);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.length * 4, java.nio.ByteBuffer.allocateDirect(vertices.length * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().put(vertices).position(0), GLES30.GL_STATIC_DRAW);
        
        // Position attribute
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 8 * 4, 0);
        GLES30.glEnableVertexAttribArray(0);
        
        // TexCoord attribute
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 8 * 4, 3 * 4);
        GLES30.glEnableVertexAttribArray(1);
        
        // Normal attribute
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, 8 * 4, 5 * 4);
        GLES30.glEnableVertexAttribArray(2);
        
        GLES30.glBindVertexArray(0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
    }
    
    private void checkGlError(String operation) {
        int error;
        while ((error = GLES30.glGetError()) != GLES30.GL_NO_ERROR) {
            Log.e(TAG, operation + ": glError " + error);
        }
    }
}
