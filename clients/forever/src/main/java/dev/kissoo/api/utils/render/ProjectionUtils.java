package dev.kissoo.api.utils.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ProjectionUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public static Vector3f project(Vec3d worldPos) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        Vector3f target = new Vector3f(
                (float) (worldPos.x - camPos.x),
                (float) (worldPos.y - camPos.y),
                (float) (worldPos.z - camPos.z)
        );
        Quaternionf rotation = new Quaternionf(camera.getRotation());
        rotation.conjugate();
        target.rotate(rotation);
        double fov = mc.options.getFov().getValue();
        float fovRad = (float) Math.toRadians(fov);
        float aspectRatio = (float) mc.getWindow().getFramebufferWidth() / (float) mc.getWindow().getFramebufferHeight();
        float near = 0.05f;
        float far = 1000.0f;
        float tanHalfFov = (float) Math.tan(fovRad / 2.0f);
        float x = target.x / (tanHalfFov * aspectRatio);
        float y = target.y / tanHalfFov;
        float z = target.z; // View Space Z
        if (z >= 0) {
            return null; 
        }
        float halfWidth = mc.getWindow().getScaledWidth() / 2f;
        float halfHeight = mc.getWindow().getScaledHeight() / 2f;
        float screenX = halfWidth + (x / -z) * halfWidth;
        float screenY = halfHeight - (y / -z) * halfHeight;

        return new Vector3f(screenX, screenY, -z);
    }
}