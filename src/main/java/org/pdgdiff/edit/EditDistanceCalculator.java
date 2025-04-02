package org.pdgdiff.edit;

import org.pdgdiff.edit.model.EditOperation;

import java.util.List;

public class EditDistanceCalculator {

    public static int calculateEditDistance(List<EditOperation> editScript) {
        int distance = 0;
        for (EditOperation op : editScript) {
            switch (op.getName()) {
                case "Insert":
                case "Delete":
                case "Update":
                case "Move":
                    distance += 1;
                    break;
                default:
                    break;
            }
        }
        return distance;
    }
}
