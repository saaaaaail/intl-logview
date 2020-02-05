package com.iqiyi.intl.logview.base;

public class Tuple3<V1, V2, V3> extends Tuple2<V1, V2> {
    public final V3 v3;

    public Tuple3(V1 v1, V2 v2, V3 v3) {
        super(v1, v2);
        this.v3 = v3;
    }

    public V3 getV3() {
        return v3;
    }

}
