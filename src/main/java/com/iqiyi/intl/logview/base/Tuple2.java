package com.iqiyi.intl.logview.base;

public class Tuple2<V1, V2> extends Tuple {
    public final V1 v1;
    public final V2 v2;

    public Tuple2(V1 v1, V2 v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    public V1 getV1() {
        return v1;
    }

    public V2 getV2() {
        return v2;
    }
}
