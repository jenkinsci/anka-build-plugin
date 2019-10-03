package com.veertu;


import java.util.List;

public class RoundRobin {

    public static final int startWeight = 50;
    private final int penalty = startWeight/4;
    private final Object valuesLock = new Object();
    private final Object lock = new Object();
    private int total;
    private String[] urlMap;
    private WeighedURL[] values;
    private int index;
    private final int roundsForOptimization = 10;

    public RoundRobin(List<String> stringValues) {
        this.values = new WeighedURL[stringValues.size()];
        for (int i = 0; i < stringValues.size(); i++) {
            this.values[i] = new WeighedURL(stringValues.get(i));
        }
        this.calculate();
    }

    public void calculate() {
        synchronized (this.valuesLock) {
            synchronized (this.lock) {
                this.total = 0;
                int[] counters = new int[this.values.length];
                for (int i = 0; i < this.values.length; i++) {
                    WeighedURL ep = this.values[i];
                    counters[i] = ep.getWeight();
                    this.total += ep.getWeight();
                }
                this.urlMap = new String[this.total];
                int idx = 0;
                while (idx < this.total) {
                   for (int j = 0; j < counters.length; j++) {
                       if (counters[j] > 0) {
                           this.urlMap[idx] =  this.values[j].getUrl();
                           idx++;
                           counters[j]--;
                       }
                   }
                }
            }
        }
    }

    public String next() {
        synchronized (this.lock) {
            try {
                String val = this.urlMap[this.index % this.total];
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (RoundRobin.this.index % RoundRobin.this.roundsForOptimization == 0) {
                            RoundRobin.this.optimize();
                        }
                    }
                }).start();


                return val;
            } finally {
                this.index++;
            }
        }
    }

    private void optimize() {
        synchronized (this.valuesLock) {
            WeighedURL last = null;
            for (int i = 0; i < this.values.length; i++) {
                if (!this.values[i].isFailed()) {
                    if (last != null) {
                        float change = calculateChange(last.getLatency(), this.values[i].getLatency());
                        float adjust = 1 + (-change);
                        int newWeight = (int) ((last.getWeight()) * adjust);
                        if (newWeight > 0) {
                            this.values[i].setWeight(newWeight);
                        } else {
                            this.values[i].setWeight(1);
                        }
                    } else {
                        last = this.values[i];
                        this.values[i].setWeight(startWeight);
                    }
                }
            }
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                RoundRobin.this.calculate();
            }
        }).start();
    }

    private float calculateChange(float v1, float v2) {
        return ((v2 - v1) / v1 );
    }

    public void fail(WeighedURL ep) {
        synchronized (this.valuesLock) {
            ep.setFailed(true);
            int newWeight = ep.getWeight() - penalty;
            if (newWeight <= 0) {
                ep.setWeight(1);
            } else {
                ep.setWeight(newWeight);
            }
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                RoundRobin.this.calculate();
            }
        }).start();
    }

    public void update(String host, int latency, boolean failed) {
        for (int i = 0; i < this.values.length; i++) {
            if (this.values[i].getUrl().equals(host)) {
                if (failed) {
                    this.fail(this.values[i]);
                    return;
                }
                synchronized (valuesLock) {
                    this.values[i].setFailed(false);
                    if (latency > 0) {
                        this.values[i].setLatency(latency);
                    }
                    if (this.values[i].getLatency() < 1) {
                        this.values[i].setLatency(1);
                    }
                    return;
                }
            }
        }
    }
}
