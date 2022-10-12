package org.keycloak.plugins.groups.helpers;

import org.keycloak.models.GroupModel;
import org.keycloak.plugins.groups.representations.GroupRepresentation;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ModelToRepresentation extends org.keycloak.models.utils.ModelToRepresentation {


    /**
     * change logic from Keycloak ModelToRepresentation
     * We do not want all related groups lists
     * based on full create or not the path
     * @param group
     * @param full
     * @return
     */
    public static GroupRepresentation toRepresentation(GroupModel group, boolean full, boolean hasRights) {
        GroupRepresentation rep = new GroupRepresentation();
        rep.setId(group.getId());
        rep.setName(group.getName());
        rep.setHasRights(hasRights);
        if ( full)
           rep.setPath(buildGroupPath(group));
        return rep;
    }

    public static GroupRepresentation toGroupHierarchy(GroupModel group, boolean full) {
        GroupRepresentation rep = toSimpleGroupHierarchy(group, full);
        if ( group.getParent() != null ) {
            GroupModel parentGroup = group.getParent();
            do {
                GroupRepresentation repChild = rep;
                rep = toRepresentation(parentGroup, full, false);
                rep.setExtraSubGroups(Stream.of(repChild).collect(Collectors.toList()));
                parentGroup = parentGroup.getParent();
            } while(parentGroup != null);
        }
        return rep;
    }

    public static GroupRepresentation toSimpleGroupHierarchy(GroupModel group, boolean full) {
        GroupRepresentation rep = toRepresentation(group, full, true);
        List<GroupRepresentation> subGroups = group.getSubGroupsStream()
                .map(subGroup -> toSimpleGroupHierarchy(subGroup, full)).collect(Collectors.toList());
        rep.setExtraSubGroups(subGroups);
        return rep;
    }



}
