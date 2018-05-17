package com.uber.okbuck.core.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.stream.Collector;

public final class MoreCollectors {

  private MoreCollectors() {}

  /**
   * Returns a {@code Collector} that builds an {@code ImmutableList}.
   *
   * <p>This {@code Collector} behaves similar to {@code
   * Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf)} but without building
   * the intermediate list.
   *
   * @param <T> the type of the input elements
   * @return a {@code Collector} that builds an {@code ImmutableList}.
   */
  public static <T> Collector<T, ?, ImmutableList<T>> toImmutableList() {
    return Collector.<T, ImmutableList.Builder<T>, ImmutableList<T>>of(
        ImmutableList::builder,
        ImmutableList.Builder::add,
        (left, right) -> left.addAll(right.build()),
        ImmutableList.Builder::build);
  }

  /**
   * Returns a {@code Collector} that builds an {@code ImmutableSet}.
   *
   * <p>This {@code Collector} behaves similar to {@code
   * Collectors.collectingAndThen(Collectors.toList(), ImmutableSet::copyOf)} but without building
   * the intermediate list.
   *
   * @param <T> the type of the input elements
   * @return a {@code Collector} that builds an {@code ImmutableSet}.
   */
  public static <T> Collector<T, ?, ImmutableSet<T>> toImmutableSet() {
    return Collector.<T, ImmutableSet.Builder<T>, ImmutableSet<T>>of(
        ImmutableSet::builder,
        ImmutableSet.Builder::add,
        (left, right) -> left.addAll(right.build()),
        ImmutableSet.Builder::build);
  }
}
