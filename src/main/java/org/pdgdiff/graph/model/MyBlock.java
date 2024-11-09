package org.pdgdiff.graph.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


// Follows how soot does it, a block is a collection of units (i.e. block of code)
public class MyBlock implements Iterable<MyUnit> {

    private List<MyUnit> units;

    public MyBlock() {
        this.units = new ArrayList<>();
    }

    public MyBlock(List<MyUnit> units) {
        this.units = units;
    }

    public List<MyUnit> getUnits() {
        return units;
    }

    public void addUnit(MyUnit unit) {
        units.add(unit);
    }

    @Override
    public Iterator<MyUnit> iterator() {
        return units.iterator();
    }

    public MyUnit getHead() {
        if (!units.isEmpty()) {
            return units.get(0);
        }
        return null;
    }
}
