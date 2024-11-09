package org.pdgdiff.graph.model;

import java.util.List;

public interface MyRegion {

    int getID();

    MyUnit getFirst();

    MyUnit getLast();

    List<MyUnit> getUnits();

    List<MyUnit> getUnits(MyUnit from, MyUnit to);

    List<MyBlock> getBlocks();

    void setParent(MyRegion parent);

    MyRegion getParent();

    void addChildRegion(MyRegion child);

    List<MyRegion> getChildRegions();
}
