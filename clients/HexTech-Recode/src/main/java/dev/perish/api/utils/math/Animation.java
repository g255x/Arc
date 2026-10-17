package dev.perish.api.utils.math;

public class Animation {
    private final FadeUtils fadeUtils = new FadeUtils(0L);
    public double from = 0.0;
    public double to = 0.0;
    private double velocity = 0.0;
    private boolean useSpring = false;
    private double springStiffness = 170.0;
    private double springDamping = 14.0;

    public Animation() {
    }

    public Animation(boolean useSpring) {
        this.useSpring = useSpring;
    }

    public double get(double target, long length, Easing ease) {
        if (useSpring) {
            return getSpring(target);
        }
        if (target != this.to) {
            this.from = this.from + (this.to - this.from) * this.fadeUtils.ease(ease);
            this.to = target;
            this.fadeUtils.reset();
        }

        this.fadeUtils.setLength(length);
        return this.from + (this.to - this.from) * this.fadeUtils.ease(ease);
    }

    public double getSpring(double target) {
        double delta = target - this.from;
        double force = delta * springStiffness;
        double dampingForce = this.velocity * springDamping;
        this.velocity += (force - dampingForce) * 0.016;
        this.from += this.velocity * 0.016;
        if (Math.abs(delta) < 0.001 && Math.abs(this.velocity) < 0.001) {
            this.from = target;
            this.velocity = 0;
        }
        this.to = target;
        return this.from;
    }

    public void setSpringParameters(double stiffness, double damping) {
        this.springStiffness = stiffness;
        this.springDamping = damping;
    }

    public void reset() {
        this.from = 0.0;
        this.to = 0.0;
        this.velocity = 0.0;
        this.fadeUtils.reset();
    }

    public boolean isFinished() {
        if (useSpring) {
            return Math.abs(this.to - this.from) < 0.001 && Math.abs(this.velocity) < 0.001;
        }
        return this.fadeUtils.isEnd();
    }
}
