package ru.icmmg.rnastruct.builder;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TreeItem {
  public static final int CONS_STICKOUT_MAX = 16;

  public static final int F = 0;
  public static final int L = 1;
  public static final int S = 2;
  public static final int E = 3;
  private int Type;
  private boolean modifiableRootStem = false;
  private boolean closedRootStem = false;
  private boolean removableStem = true;

  List<TreeItem> childs = new LinkedList<>();
  private TreeItem parent;
  private Options options;

  public TreeItem(int type, String opt) throws Exception {
    Type = type;
    if (Type == S && Pattern.compile("S\\[m\\]").matcher(opt).find()) {
      modifiableRootStem = true;
    }
    if (Type == S && Pattern.compile("S\\[!\\]").matcher(opt).find()) {
      removableStem = false;
    }
    if (Type == S && Pattern.compile("S\\[n\\]").matcher(opt).find()) {
      closedRootStem = true;
    }
    options = new Options(opt);
  }

  public TreeItem(TreeItem c) {
    this.Type = c.Type;
    this.options = new Options(c.options);
    this.modifiableRootStem = c.modifiableRootStem;
    this.removableStem = c.removableStem;
    this.closedRootStem = c.closedRootStem;
    for (TreeItem t : c.childs) {
      this.addChild(new TreeItem(t));
    }
  }

  public void addChild(TreeItem child) throws UnsupportedOperationException {
    if (Type == TreeItem.F | Type == TreeItem.S) {
      child.parent = this;
      childs.add(child);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  public int getType() {
    return Type;
  }

  public void setType(int type) {
    this.Type = type;
  }

  public List<TreeItem> getChilds() {
    return childs;
  }

  public String getOptionsString() {
    return options.toString();
  }

  public TreeItem modifyRandomOption(boolean timeDecrease) {
    if (Type != F) {
      throw new UnsupportedOperationException();
    }
    TreeItem t = new TreeItem(this);
    List<TreeItem> modified = new ArrayList<>();
    recurse(modified, t);
    int r = RandomSingleton.getInstance().nextInt(modified.size());
    modified.get(r).options.modify(timeDecrease);
    return t;
  }

  private void recurse(List<TreeItem> mod, TreeItem item) {
    if (item.isRandomModifiable()) {
      mod.add(item);
    }
    for (TreeItem i : item.childs) {
      recurse(mod, i);
    }
  }

  private void modifyThisItem(boolean timeDecrease) {
    if (RandomSingleton.getInstance().nextBoolean()) {
      this.options.modify(timeDecrease);
    }
  }

  public boolean isRemovableStem() {
    return removableStem;
  }

  public boolean isRandomModifiable() {
    return options.isModifiable();
  }

  public boolean isModifiableRootStem() {
    return modifiableRootStem;
  }

  public void setModifiableStem(boolean modifiableStem) {
    this.modifiableRootStem = modifiableStem;
  }

  public TreeItem getParent() {
    return parent;
  }

  public void setParent(TreeItem i) {
    parent = i;
  }

  public boolean isClosedRootStem() {
    return closedRootStem;
  }


  private static class Options {

    private List<Param> params = new ArrayList<>(20);

    private final static Pattern lenPat = Pattern.compile(" len:([^\\s]+)");
    private final static Pattern MslPat = Pattern.compile(" msl:([^\\s]+)");

    private Options(Options c) {
      for (Param p : c.params) {
        if (p instanceof LenParam) {
          this.params.add(new LenParam((LenParam) p));
        } else if (p instanceof ConsParam) {
          this.params.add(new ConsParam((ConsParam) p));
        } else if (p instanceof MmParam) {
          this.params.add(new MmParam((MmParam) p));
        } else if (p instanceof DistParam) {
          this.params.add(new DistParam((DistParam) p));
        } else if (p instanceof MslParam) {
          this.params.add(new MslParam((MslParam) p));
        }
      }
    }


    private Options(String opt) throws Exception {

      Matcher l = lenPat.matcher(opt);
      if (l.find()) {
        params.add(new LenParam(" len", l.group(1)));
      }

      addCons("cons_3", "mm_3", "dist_3", opt);
      addCons("cons_5", "mm_5", "dist_5", opt);
      addCons("L_cons_3", "L_mm_3", "L_dist_3", opt);
      addCons("R_cons_5", "R_mm_5", "R_dist_5", opt);
      addCons("L_cons_5", "L_mm_5", "L_dist_5", opt);
      addCons("R_cons_3", "R_mm_3", "R_dist_3", opt);


      Matcher m = MslPat.matcher(opt);
      if (m.find()) {
        params.add(new MslParam(" msl", m.group(1), (LenParam) params.get(0)));
      }

    }

    private void addCons(String consName, String mmName, String distName, String opts) throws Exception {
      Pattern consP = Pattern.compile(" " + consName + ":([^\\s]+)");
      Pattern mmP = Pattern.compile(" " + mmName + ":([^\\s]+)");
      Pattern distP = Pattern.compile(" " + distName + ":([^\\s]+)");

      Matcher consM = consP.matcher(opts);
      if (consM.find()) {
        ConsParam p = new ConsParam(consName, consM.group(1));
        params.add(p);

        Matcher mmM = mmP.matcher(opts);
        if (mmM.find()) {
          params.add(new MmParam(mmName, mmM.group(1), p));
        }

        Matcher distM = distP.matcher(opts);
        if (distM.find()) {
          LenParam lp = (LenParam) params.get(0);
          params.add(new DistParam(distName, distM.group(1), lp, p));
        }
      }

    }


    @Override
    public String toString() {
      StringBuilder s = new StringBuilder(100);
      for (Param p : params) {
        s.append(p.toString()).append("  ");
      }

      return s.toString();
    }

    private boolean isModifiable() {
      if (params.isEmpty()) {
        return false;
      } else {
        for (Param p : params) {
          if (p.isModifiable()) {
            return true;
          }
        }
        return false;
      }
    }


    /*
    возвращает true, если модификация прошла успешно, и false, если
    нечего модифицировать (все параметры = null)
     */
    private boolean modify(boolean timeDecrease) {
      if (!isModifiable()) {
        return false;
      }

      int r = RandomSingleton.getInstance().nextInt(params.size());
      while (!params.get(r).isModifiable()) {
        r = RandomSingleton.getInstance().nextInt(params.size());
      }

      params.get(r).modify(timeDecrease);
      return true;

    }


    private abstract class Param {
      protected String name;
      protected boolean modifiable = false;
      protected int startMod;
      protected int endMod;

      private Param(Param p) {
        name = p.name;
        modifiable = p.modifiable;
        startMod = p.startMod;
        endMod = p.endMod;
      }

      private Param(String name, int startMod, int endMod) {
        this.name = name;
        this.startMod = startMod;
        this.endMod = endMod;
      }

      public boolean isModifiable() {
        return modifiable;
      }

      public void modify(boolean timeDecrease) {
        throw new UnsupportedOperationException();
      }

      protected void setModifyParams(String params) {
        if (params != null) {
          String g2 = params.substring(1, params.length() - 1);
          if (g2.equals("!")) {
            modifiable = false;
          } else {
            Matcher m = Pattern.compile("(\\d+)\\.\\.(\\d+)").matcher(g2);
            if (m.find()) {
              modifiable = true;
              startMod = Integer.valueOf(m.group(1));
              endMod = Integer.valueOf(m.group(2));
            } else {
              throw new UnsupportedOperationException();
            }
          }
        }
      }

      abstract void parseString(String str) throws Exception;

      abstract String paramToString();

      protected int getRandom(int s, int e) {

        return s + RandomSingleton.getInstance().nextInt(e - s + 1);
      }

      @Override
      public String toString() {
        return name + ":" + paramToString();
      }
    }


    private class MmParam extends Param{
      private int mm;
      private ConsParam cons;

      public MmParam(String name, String init, ConsParam cons) throws Exception {
        super(name, 0, cons.getCons().length());
        this.cons = cons;
        modifiable = true;
        parseString(init);
      }

      public MmParam(MmParam p) {
        super(p);
        mm = p.mm;
        cons = new ConsParam(p.cons);
      }

      @Override
      public void modify(boolean timeDecrease) {
        if (!modifiable) {
          return;
        }
        if (!timeDecrease) {
          mm = getRandom(startMod, Math.min(endMod, cons.getCons().length()));
        } else {
          mm = mm - 1;
        }
      }

      public void parseString(String str) throws Exception {
        Matcher m = Pattern.compile("(\\d+)\\(w:-10\\)(\\[[^\\s]+\\])?").matcher(str);
        if (m.find()) {
          mm = Integer.valueOf(m.group(1));
          setModifyParams(m.group(2));
        }
      }

      @Override
      String paramToString() {
        return mm + "(w:-10)";
      }
    }


    private class DistParam extends Param {
      private int start;
      private int end;
      private LenParam len;
      private ConsParam cons;

      public DistParam(DistParam p) {
        super(p);
        start = p.start;
        end = p.end;
        len = new LenParam(p.len);
        cons = new ConsParam(p.cons);
      }

      public DistParam(String name, String init, LenParam len, ConsParam cons) throws Exception {
        super(name, 0, Math.min(CONS_STICKOUT_MAX, len.maxLen));
        this.len = len;
        this.cons = cons;
        modifiable = true;
        parseString(init);
      }

      @Override
      public void modify(boolean timeDecrease) {
        if (!modifiable) {
          return;
        }
        Random r = RandomSingleton.getInstance();
        if (r.nextBoolean()) {
          if (!timeDecrease) {
            start = getRandom(startMod, Math.min(end, endMod));
          } else {
            start = start + 1;
          }
        } else {
          if (!timeDecrease) {
            end = getRandom(start, Math.min(endMod, len.maxLen));
          } else {
            end = end - 1;
          }
        }
      }

      @Override
      void parseString(String str) throws Exception {
        Pattern pat = Pattern.compile("(\\d+)\\.\\.(\\d+)(\\[[^\\s]+\\])?");
        Matcher m = pat.matcher(str);
        if (m.find()) {
          start = Integer.valueOf(m.group(1));
          end = Integer.valueOf(m.group(2));
          setModifyParams(m.group(3));
        } else {
          throw new Exception();
        }
      }

      @Override
      String paramToString() {
        return start + ".." + end;
      }
    }


    private class ConsParam extends Param {
      private String cons;

      public ConsParam(ConsParam p) {
        super(p);
        this.cons = p.cons;
      }

      public ConsParam(String name, String init) throws Exception {
        super(name, 0, 0);
        parseString(init);
      }

      @Override
      void parseString(String str) throws Exception {
        Pattern pat = Pattern.compile("([\\w]+)");
        Matcher m = pat.matcher(str);
        if (m.find()) {
          cons = m.group(1);
        } else {
          throw new Exception();
        }
      }

      public String getCons() {
        return cons;
      }

      @Override
      String paramToString() {
        return cons;
      }
    }


    private class LenParam extends Param {
      private int minLen;
      private int maxLen;

      public LenParam(LenParam p) {
        super(p);
        minLen = p.minLen;
        maxLen = p.maxLen;
      }

      public LenParam(String name, String init) throws Exception {
        super(name, 0, 30);
        modifiable = true;
        parseString(init);
      }

      public int[] getLen() {
        return new int[] {minLen, maxLen};
      }

      @Override
      public void modify(boolean timeDecrease) {
        if (!modifiable) {
          return;
        }
        Random r = RandomSingleton.getInstance();
        if (r.nextBoolean()) {
          if (!timeDecrease) {
            minLen = getRandom(startMod, Math.min(endMod, maxLen));
          } else {
            minLen = minLen + 1;
          }
        } else {
          if (!timeDecrease) {
            maxLen = getRandom(minLen, endMod);
          } else {
            maxLen = maxLen - 1;
          }
        }
      }

      @Override
      void parseString(String str) throws Exception {
        Pattern pat = Pattern.compile("(\\d+)\\.\\.(\\d+)(\\[[^\\s]+\\])?");
        Matcher m = pat.matcher(str);
        if (m.find()) {
          minLen = Integer.valueOf(m.group(1));
          maxLen = Integer.valueOf(m.group(2));
          setModifyParams(m.group(3));
        } else {
          throw new Exception();
        }
      }

      @Override
      String paramToString() {
        return minLen + ".." + maxLen;
      }
    }


    private class MslParam extends Param {
      private int msl;
      private LenParam len;

      public MslParam(MslParam p) {
        super(p);
        msl = p.msl;
        len = new LenParam(p.len);
      }

      public MslParam(String name, String init, LenParam len) throws Exception {
        super(name, 0, len.maxLen);
        this.len = len;
        modifiable = true;
        parseString(init);
      }

      @Override
      public void modify(boolean timeDecrease) {
        if (!modifiable) {
          return;
        }
        if (!timeDecrease) {
          msl = getRandom(startMod, Math.min(endMod, len.getLen()[1]));
        } else {
          msl = msl - 1;
        }
      }

      @Override
      void parseString(String str) throws Exception {
        Pattern pat = Pattern.compile("(\\d+)(\\[[^\\s]+\\])?");
        Matcher m = pat.matcher(str);
        if (m.find()) {
          msl = Integer.valueOf(m.group(1));
          setModifyParams(m.group(2));
        } else {
          throw new Exception();
        }
      }

      @Override
      String paramToString() {
        return msl + "";
      }
    }


  }


}