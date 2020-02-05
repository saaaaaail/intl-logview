package com.iqiyi.intl.logview.base;

public class Tuple {

    public static <V1, V2> Tuple2<V1, V2> apply(V1 v1, V2 v2) {
        return new Tuple2<V1, V2>(v1, v2);
    }

    public static <V1, V2, V3> Tuple3<V1, V2, V3> apply(V1 v1, V2 v2, V3 v3) {
        return new Tuple3<>(v1, v2, v3);
    }


}
