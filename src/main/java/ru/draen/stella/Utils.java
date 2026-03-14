package ru.draen.stella;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Utils {
    public static<T> Stream<List<T>> productList(List<List<T>> items) {
        return cartesianProductList(items,0);
    }
    public static<T> Stream<List<T>> productStream(List<Stream<T>> items) {
        return cartesianProductStream(items,0);
    }

    private static<T> Stream<List<T>> cartesianProductStream(List<Stream<T>> sets, int index) {
        if (index == sets.size()) {
            List<T> emptyList = new ArrayList<>();
            return Stream.of(emptyList);
        }
        Stream<T> currentSet = sets.get(index);
        return currentSet.flatMap(element -> cartesianProductStream(sets, index+1)
                .map(list -> {
                    List<T> newList = new ArrayList<>(list);
                    newList.addFirst(element);
                    return newList;
                }));
    }

    private static<T> Stream<List<T>> cartesianProductList(List<List<T>> sets, int index) {
        if (index == sets.size()) {
            List<T> emptyList = new ArrayList<>();
            return Stream.of(emptyList);
        }
        List<T> currentSet = sets.get(index);
        return currentSet.stream().flatMap(element -> cartesianProductList(sets, index+1)
                .map(list -> {
                    List<T> newList = new ArrayList<>(list);
                    newList.addFirst(element);
                    return newList;
                }));
    }
}
