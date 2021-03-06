package dev.ttetris.view.material;

import dev.ttetris.util.AssetManager;
import dev.ttetris.util.Attribute;
import dev.ttetris.util.DepthTest;
import dev.ttetris.util.Material;

public class BackgroundMaterial extends Material {
    public BackgroundMaterial(AssetManager paramAssetManager, String paramString) {
        super(paramAssetManager.getShader("Shaders/Background.glsl"));
        super.addTexture(paramAssetManager.getTexture(paramString), "sBaseMap");
        addAttribute(Attribute.POSITION, "aPosition");
        this.depthTest = DepthTest.NONE;
        this.depthMask = false;
    }
}
