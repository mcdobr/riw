package me.mircea.riw.util;

import com.google.common.base.Preconditions;

import java.util.List;

public class LinearAlgebraUtil {
    // TODO: should've written unit tests before writing this... guess after

    public static double cosine(List<Double> u, List<Double> v) {
        Preconditions.checkNotNull(u);
        Preconditions.checkNotNull(v);

        return dotProduct(u, v) / norm(u) / norm(v);
    }


    public static double dotProduct(List<Double> u, List<Double> v) {
        Preconditions.checkNotNull(u);
        Preconditions.checkNotNull(v);

        final int dimensions = Math.min(u.size(), v.size());

        double acc = 0;
        for (int i = 0; i < dimensions; ++i) {
            acc += u.get(i) * v.get(i);
        }
        return acc;
    }

    public static double norm(List<Double> u) {
        Preconditions.checkNotNull(u);

        double acc = 0;
        for (int i = 0; i < u.size(); ++i)
            acc += u.get(i) * u.get(i);

        return Math.sqrt(acc);
    }
}
