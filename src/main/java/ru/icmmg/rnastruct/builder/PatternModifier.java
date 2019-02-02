package ru.icmmg.rnastruct.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

public class PatternModifier {
  public static final String NEW_STEM_PARAMS = " len:2..20   msl:0[!]";
  public static final String NEW_TERMINAL_LOOP_PARAMS = " len:3..10";
  public static final String NEW_LOOP_PARAMS = " len:0..10";

  public static TreeItem modify_(TreeItem f, boolean enableModifySturcute, boolean timeDecrease) {
    int count = (int) Math.abs(RandomSingleton.getInstance().nextGaussian() * 10) + 1;
    System.out.println("Modify steps count: " + count);
    for (int i = 0; i < count; i++) {
      f = modify(f, enableModifySturcute, timeDecrease);
    }
    return f;
  };

  public static TreeItem modify(TreeItem f, boolean enableModifySturcute, boolean timeDecrease) {
    if (!enableModifySturcute) {
      System.out.println("Modify parameters");
      return f.modifyRandomOption(timeDecrease);
    }
    if (RandomSingleton.getInstance().nextBoolean()) {
      System.out.println("Modify parameters");
      return f.modifyRandomOption(timeDecrease);
    } else {
      //System.out.println("modify structure");
      return modifyTreeStructure(new TreeItem(f));
    }
  }

  private static TreeItem modifyTreeStructure(TreeItem f) {
    List<TreeItem> loopList = new ArrayList<>();
    List<TreeItem> leafStemList = new ArrayList<>();
    List<TreeItem> stemsForDeletingList = new ArrayList<>();
    List<TreeItem> allStemList = new ArrayList<>();
    recurseCreateList(f, loopList, leafStemList, stemsForDeletingList, allStemList, false);
    if (loopList.size() + leafStemList.size() + stemsForDeletingList.size() + allStemList.size() == 0) {
      return f;
    }

    List<List<TreeItem>> rl = new ArrayList<>();
    if (!loopList.isEmpty()) {
      rl.add(loopList);
    }
    if (!leafStemList.isEmpty()) {
      rl.add(leafStemList);
    }
    if (!stemsForDeletingList.isEmpty()) {
      rl.add(stemsForDeletingList);
    }
    if (!allStemList.isEmpty()) {
      rl.add(allStemList);
    }

    List<TreeItem> randList = rl.get(RandomSingleton.getInstance().nextInt(rl.size()));

    if (randList == loopList) {
      System.out.println("modify structure - addLeafStem");
      try {
        return addLeafStem(f, loopList.get(RandomSingleton.getInstance().nextInt(loopList.size())));
      } catch (Exception e) {
        System.out.println("exception in modifyTreeStructure.addLeafStem");
        return f;
      }
    } else if (randList == leafStemList) {
      System.out.println("modify structure - removeLeafStem");
      return removeLeafStem(f, leafStemList.get(RandomSingleton.getInstance().nextInt(leafStemList.size())));
    } else if (randList == stemsForDeletingList) {
      System.out.println("modify structure - deleteMiddleStem");
      return removeMiddleStem(f, stemsForDeletingList.get(
          RandomSingleton.getInstance().nextInt(stemsForDeletingList.size())));
    } else {
      System.out.println("modify structure - addNewStem");
      try {
        return addNewStem(f, allStemList.get(RandomSingleton.getInstance().nextInt(allStemList.size())));
      } catch (Exception e) {
        System.out.println("exception in modifyTreeStructure.addNewStem");
        return f;
      }
    }

  }

  private static TreeItem removeMiddleStem(TreeItem f, TreeItem stem) {
    stem.getParent().childs = stem.getChilds();
    for (TreeItem i : stem.getParent().childs) {
      i.setParent(stem.getParent());
    }
    return f;
  }

  private static TreeItem addNewStem(TreeItem f, TreeItem stem) throws Exception {
    TreeItem l1 = new TreeItem(TreeItem.L, NEW_LOOP_PARAMS);
    TreeItem s = new TreeItem(TreeItem.S, NEW_STEM_PARAMS);
    TreeItem l2 = new TreeItem(TreeItem.L, NEW_LOOP_PARAMS);
    s.childs = stem.getChilds();
    for (TreeItem i : s.childs) {
      i.setParent(s);
    }
    l1.setParent(stem);
    s.setParent(stem);
    l2.setParent(stem);
    stem.childs = Arrays.asList(l1, s, l2);
    return f;
  }

  private static TreeItem removeLeafStem(TreeItem f, TreeItem stem) {
    TreeItem parentStem = stem.getParent();
    int ind = parentStem.getChilds().indexOf(stem);
    parentStem.getChilds().remove(ind + 1);
    parentStem.getChilds().remove(ind);
    return f;
  }

  private static TreeItem addLeafStem(TreeItem f, TreeItem loop) throws Exception {
    TreeItem parentStem = loop.getParent();
    TreeItem newStem = new TreeItem(TreeItem.S, NEW_STEM_PARAMS);
    TreeItem newLoop = new TreeItem(TreeItem.L, NEW_LOOP_PARAMS);
    newStem.addChild(new TreeItem(TreeItem.L, NEW_TERMINAL_LOOP_PARAMS));

    ListIterator<TreeItem> iter = parentStem.getChilds().listIterator();
    while (iter.hasNext()) {
      TreeItem item = iter.next();
      if (item == loop) {
        iter.add(newStem);
        iter.add(newLoop);
        newStem.setParent(loop.getParent());
        newLoop.setParent(loop.getParent());
      }
    }
    return f;
  }

  private static void recurseCreateList(TreeItem item,
                                        List<TreeItem> loopList,
                                        List<TreeItem> leafStemList,
                                        List<TreeItem> stemsForDeletingList,
                                        List<TreeItem> allStemList,
                                        boolean permit) {
    if (item.getType() == TreeItem.S && item.isClosedRootStem()) {
      return;
    }
    if (permit) {
      if (item.getType() == TreeItem.L) {
        loopList.add(item);
      }
      if (item.getType() == TreeItem.S && item.isRemovableStem() && item.getChilds().size() == 1) {
        leafStemList.add(item);
      }
      if (item.getType() == TreeItem.S && item.isRemovableStem() && item.getParent().getChilds().size() == 3) {
        stemsForDeletingList.add(item);
      }
      if (item.getType() == TreeItem.S) {
        allStemList.add(item);
      }
    }
    if (item.isModifiableRootStem()) {
      allStemList.add(item);
    }

    for (TreeItem i : item.getChilds()) {
      recurseCreateList(i, loopList, leafStemList, stemsForDeletingList, allStemList, permit || item.isModifiableRootStem());
    }
  }
}
