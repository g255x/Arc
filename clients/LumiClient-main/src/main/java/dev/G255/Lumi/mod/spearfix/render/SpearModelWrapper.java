package dev.G255.Lumi.mod.spearfix.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.List;

public class SpearModelWrapper implements BakedModel {
    private final BakedModel spearModel;
    private final BakedModel spearInHandModel;
    private final BakedModel originalModel;
    private final ModelTransformationMode displayContext;

    public SpearModelWrapper(BakedModel spearModel, BakedModel spearInHandModel,
                             BakedModel originalModel, ModelTransformationMode displayContext) {
        this.spearModel = spearModel;
        this.spearInHandModel = spearInHandModel;
        this.originalModel = originalModel;
        this.displayContext = displayContext;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction face, Random random) {
        return selectModel().getQuads(state, face, random);
    }

    @Override
    public boolean useAmbientOcclusion() {
        BakedModel m = safeModel();
        return m != null && m.useAmbientOcclusion();
    }

    @Override
    public boolean hasDepth() {
        return selectModel().hasDepth();
    }

    @Override
    public boolean isSideLit() {
        return selectModel().isSideLit();
    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

    @Override
    public Sprite getParticleSprite() {
        BakedModel m = safeModel();
        return m != null ? m.getParticleSprite() : null;
    }

    @Override
    public ModelTransformation getTransformation() {
        return selectModel().getTransformation();
    }

    @Override
    public ModelOverrideList getOverrides() {
        return ModelOverrideList.EMPTY;
    }

    @Override
    public boolean isVanillaAdapter() {
        return true;
    }

    private BakedModel selectModel() {
        if (spearInHandModel != null && displayContext != null
                && displayContext != ModelTransformationMode.GUI
                && displayContext != ModelTransformationMode.GROUND
                && displayContext != ModelTransformationMode.FIXED) {
            return spearInHandModel;
        }
        return spearModel != null ? spearModel : originalModel;
    }

    private BakedModel safeModel() {
        if (spearModel != null) return spearModel;
        if (spearInHandModel != null) return spearInHandModel;
        return originalModel;
    }
}