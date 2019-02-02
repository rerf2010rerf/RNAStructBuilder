package ru.icmmg.rnastruct.builder;

import java.util.Random;

public class RandomSingleton {
  private static Random rand = new Random();

  public static Random getInstance() {
    return rand;
  }
}
