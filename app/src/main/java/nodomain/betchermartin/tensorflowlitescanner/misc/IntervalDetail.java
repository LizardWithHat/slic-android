package nodomain.betchermartin.tensorflowlitescanner.misc;

public class IntervalDetail extends SimpleDetail {
    private int intervalMin;
    private int intervalMax;
    private int step;

    public IntervalDetail(String key, String description, String value){
        super(key, description, value);
        intervalMin = 0;
        intervalMax = 0;
        step = 0;
    }

    public int getIntervalMin() {
        return intervalMin;
    }

    public void setIntervalMin(int intervalMin) {
        this.intervalMin = intervalMin;
    }

    public int getIntervalMax() {
        return intervalMax;
    }

    public void setIntervalMax(int intervalMax) {
        this.intervalMax = intervalMax;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }
}
