package org.keycloak.plugins.groups.helpers;

import org.keycloak.models.GroupModel;
import org.keycloak.plugins.groups.representations.GroupRepresentation;

import java.util.List;
import java.util.Map;
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
    public static GroupRepresentation toRepresentation(GroupModel group, boolean full) {
        GroupRepresentation rep = new GroupRepresentation();
        rep.setId(group.getId());
        rep.setName(group.getName());
        if ( full)
           rep.setPath(buildGroupPath(group));
        return rep;
    }

    public static GroupRepresentation toRepresentationWithAttributes(GroupModel group) {
        GroupRepresentation rep = new GroupRepresentation();
        rep.setId(group.getId());
        rep.setName(group.getName());
        Map<String, List<String>> attributes = group.getAttributes();
        rep.setAttributes(attributes);
        rep.setPath(buildGroupPath(group));
        List<GroupRepresentation> subGroups = group.getSubGroupsStream()
                .map(subGroup -> toSimpleGroupHierarchy(subGroup, true)).collect(Collectors.toList());
        rep.setExtraSubGroups(subGroups);
        return rep;
    }

    public static GroupRepresentation toSimpleGroupHierarchy(GroupModel group, boolean full) {
        GroupRepresentation rep = toRepresentation(group, full);
        List<GroupRepresentation> subGroups = group.getSubGroupsStream()
                .map(subGroup -> toSimpleGroupHierarchy(subGroup, full)).collect(Collectors.toList());
        rep.setExtraSubGroups(subGroups);
        return rep;
    }


}
