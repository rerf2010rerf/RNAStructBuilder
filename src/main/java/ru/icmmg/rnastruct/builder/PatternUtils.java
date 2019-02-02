package ru.icmmg.rnastruct.builder;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternUtils {

  private static StringBuilder deep;
  private static int present;

  private static Pattern optPattern = Pattern.compile("([\\p{Alpha}_]+\\:\\-?\\d+)");

  public static String writeTree(TreeItem F){
    StringBuilder res = new StringBuilder("RNA_TREE_BEGIN\n");
    res.append("F\n");
    deep = new StringBuilder("");
    res.append(recBuildText(F, 0));
    res.append("RNA_TREE_END\n" +
      "PSEUDOKNOTS_BEGIN\n" +
      "PSEUDOKNOTS_END");
    return res.toString();
  }


  private static String recBuildText(TreeItem s, int order){
    StringBuilder res = new StringBuilder("");
    if (s.getType() == TreeItem.S){
      res.append(deep + "S");
      res.append("   " + s.getOptionsString());
      if (s.isModifiableRootStem()) {
        res.append("   ;[m]");
      }
      if (!s.isRemovableStem()) {
        res.append("   ;[!]");
      }
      res.append("\n");
    }
    deep.append(" ");
    for (int k = 0; k < s.getChilds().size(); ++k){
      TreeItem i = s.getChilds().get(k);
      if (i.getType() == TreeItem.E){
        res.append(deep + "E");
        res.append("   " + i.getOptionsString() + "\n");
      }
      else if (i.getType() == TreeItem.L){
        res.append(deep + "L");
        res.append("   " + i.getOptionsString() + "\n");
      }
      else if (i.getType() == TreeItem.S){
        res.append(recBuildText(i, k));
      }
    }
    deep.delete(deep.length() - 1, deep.length());
    return res.toString();
  }




  public static TreeItem readTree(String str, Set<TreeItem> notEmptyItem) throws Exception{
    String[] strList = split(str);

    int beginIndex = search(strList, "RNA_TREE_BEGIN");
    int endIndex = search(strList, "RNA_TREE_END");
    if (beginIndex < 0 || endIndex < 0 || beginIndex > endIndex)
      throw new Exception("Incorrect tree");
    strList = Arrays.copyOfRange(strList, beginIndex + 1, endIndex);
    if (strList.length < 3)
      throw new Exception("Incorrect tree");
    //строим корень дерева
    present = 0;
    TreeItem F = new TreeItem(TreeItem.F, "");
    if (!F.isRandomModifiable()) {
      notEmptyItem.add(F);
    }
    present++;
    String EPatStr = "\\sE\\s+len\\:(\\d+)\\.\\.(\\d+)(.*)";
    Pattern EPat = Pattern.compile(EPatStr);
    Matcher m = EPat.matcher(strList[present]);
    if (!m.matches())
      throw new Exception("Incorrect tree - E loop is not present");
    else{
      strList[present] = " L " + strList[present].substring(3);
    }
    m = EPat.matcher(strList[strList.length - 1]);
    if (!m.matches())
      throw new Exception("Incorrect tree - E loop is not present");
    else{
      strList[strList.length - 1] = " L " + strList[strList.length - 1].substring(3);
    }
    recurse(strList, F, 1, notEmptyItem);
    F.getChilds().get(0).setType(TreeItem.E);
    F.getChilds().get(F.getChilds().size() - 1).setType(TreeItem.E);
    return F;
  }

  /*
  рекурсивная функция для постоения поддерева, имеющего корнем
  стебель S и находящегося на глубине deep от основного корня
  */
  private static void recurse(String[] strList, TreeItem S, int deep, Set<TreeItem> notEmpty)
      throws Exception{

    String LPatStr = "\\s{" + String.valueOf(deep) +
      "}L\\s+len\\:(\\d+)\\.\\.(\\d+)(.*)";
    String LPatStrRet = "\\s{" + String.valueOf(deep-1) +
      "}L\\s+len\\:(\\d+)\\.\\.(\\d+)(.*)";
    String SPatStr = "\\s{" + String.valueOf(deep) +
      "}S.*";

    Pattern LPat = Pattern.compile(LPatStr);
    Pattern LPatRet = Pattern.compile(LPatStrRet);
    Pattern SPat = Pattern.compile(SPatStr);
    boolean waitL = true;
    for(;;){
      if (present == strList.length)
        return;
      String str = strList[present++];
      if (waitL){
        Matcher m = LPat.matcher(str);
        if (!m.matches())
          throw new Exception("Incorrect tree");
        else{
          String opt = m.group(0);
          TreeItem newItem = new TreeItem(TreeItem.L, opt);
          S.addChild(newItem);
          if (!newItem.isRandomModifiable()) {
            notEmpty.add(newItem);
          }
          waitL = false;
        }
      }
      else {
        Matcher m = SPat.matcher(str);
        Matcher mRet = LPatRet.matcher(str);
        if (m.matches()){
          String opt = m.group(0);
          TreeItem newS = new TreeItem(TreeItem.S, opt);
          S.addChild(newS);
          if (!newS.isRandomModifiable()) {
            notEmpty.add(newS);
          }
          recurse(strList, newS, deep + 1, notEmpty);
          waitL = true;
        }
        else if (mRet.matches()){
          present--;
          return;
        }
        else
          throw new Exception("Incorrect tree");
      }
    }
  }



  private static int search(String[] arr, String val){
    for (int i = 0; i < arr.length; i++){
      String s = arr[i];
      if (s.equals(val))
        return i;
    }
    return -1;
  }

  private static String[] split(String str){
    //return str.split(System.getProperty("line.separator"));
    return str.split("\n");
  }
}
