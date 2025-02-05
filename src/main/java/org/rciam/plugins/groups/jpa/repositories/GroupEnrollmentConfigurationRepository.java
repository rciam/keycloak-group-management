package org.rciam.plugins.groups.jpa.repositories;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.TypedQuery;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.rciam.plugins.groups.enums.GroupAupTypeEnum;
import org.rciam.plugins.groups.enums.GroupTypeEnum;
import org.rciam.plugins.groups.helpers.Utils;
import org.rciam.plugins.groups.jpa.entities.GroupAupEntity;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentConfigurationRulesEntity;
import org.rciam.plugins.groups.jpa.entities.GroupRolesEntity;
import org.rciam.plugins.groups.representations.GroupAupRepresentation;
import org.rciam.plugins.groups.representations.GroupEnrollmentConfigurationRepresentation;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

public class GroupEnrollmentConfigurationRepository extends GeneralRepository<GroupEnrollmentConfigurationEntity> {

    private GroupRolesRepository groupRolesRepository;
    private final GroupEnrollmentConfigurationRulesRepository groupEnrollmentConfigurationRulesRepository;

    public GroupEnrollmentConfigurationRepository(KeycloakSession session, RealmModel realm) {
        super(session, realm);
        this.groupEnrollmentConfigurationRulesRepository = new GroupEnrollmentConfigurationRulesRepository(session);
    }

    public void setGroupRolesRepository(GroupRolesRepository groupRolesRepository) {
        this.groupRolesRepository = groupRolesRepository;
    }

    @Override
    protected Class<GroupEnrollmentConfigurationEntity> getTClass() {
        return GroupEnrollmentConfigurationEntity.class;
    }

    public void create(GroupEnrollmentConfigurationRepresentation rep, String groupId) {
        GroupEnrollmentConfigurationEntity entity = new GroupEnrollmentConfigurationEntity();
        entity.setId(KeycloakModelUtils.generateId());
        GroupEntity group = new GroupEntity();
        group.setId(groupId);
        entity.setGroup(group);
        toEntity(entity, rep, groupId);
        create(entity);
        rep.setId(entity.getId());
    }

    public void createDefault(GroupModel group, String groupName, String realmId) {
        //default values, hide by default
        List<GroupEnrollmentConfigurationRulesEntity> configurationRulesList = groupEnrollmentConfigurationRulesRepository.getByRealmAndType(realmId, group.getParentId() == null ? GroupTypeEnum.TOP_LEVEL : GroupTypeEnum.SUBGROUP).collect(Collectors.toList());
        GroupEnrollmentConfigurationEntity entity = new GroupEnrollmentConfigurationEntity();
        entity.setId(KeycloakModelUtils.generateId());
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId(group.getId());
        entity.setGroup(groupEntity);
        entity.setName("Join "+groupName);
        entity.setRequireApproval(configurationRulesList.stream().noneMatch(x -> "requireApproval".equals(x.getField()) && "false".equals(x.getDefaultValue())) );
        entity.setRequireApprovalForExtension(configurationRulesList.stream().noneMatch(x -> "requireApprovalForExtension".equals(x.getField()) &&  "false".equals(x.getDefaultValue())) );
        entity.setActive(configurationRulesList.stream().noneMatch(x -> "active".equals(x.getField()) &&  "false".equals(x.getDefaultValue())));
        entity.setVisibleToNotMembers(configurationRulesList.stream().anyMatch(x -> "visibleToNotMembers".equals(x.getField()) &&  "true".equals(x.getDefaultValue())) );
        entity.setMultiselectRole(configurationRulesList.stream().noneMatch(x -> "multiselectRole".equals(x.getField()) &&  "false".equals(x.getDefaultValue())));
        entity.setGroupRoles(groupRolesRepository.getGroupRolesByGroup(group.getId()).map(x -> {
            GroupRolesEntity r = new GroupRolesEntity();
            r.setId(x.getId());
            r.setGroup(x.getGroup());
            r.setName(x.getName());
            return r;
        }).collect(Collectors.toList()));
        entity.setCommentsNeeded(configurationRulesList.stream().noneMatch(x -> "commentsNeeded".equals(x.getField()) &&  "false".equals(x.getDefaultValue())) );
        if (entity.getCommentsNeeded()) {
            String label = configurationRulesList.stream().filter(x -> "commentsLabel".equals(x.getField())).findAny().orElse(new GroupEnrollmentConfigurationRulesEntity()).getDefaultValue();
            entity.setCommentsLabel(label != null ? label : "Comments");
            String description = configurationRulesList.stream().filter(x -> "commentsDescription".equals(x.getField())).findAny().orElse(new GroupEnrollmentConfigurationRulesEntity()).getDefaultValue();
            entity.setCommentsDescription(description != null ? description : "Why do you want to join the group?");
        }
        String membershipExpirationDaysDefault = configurationRulesList.stream().filter(x -> "membershipExpirationDays".equals(x.getField())).findAny().orElse(new GroupEnrollmentConfigurationRulesEntity()).getDefaultValue();
        if (membershipExpirationDaysDefault != null)
            entity.setMembershipExpirationDays(Long.valueOf(membershipExpirationDaysDefault));
        String aupDefault = configurationRulesList.stream().filter(x -> "aupEntity".equals(x.getField())).findAny().orElse(new GroupEnrollmentConfigurationRulesEntity()).getDefaultValue();
        if (aupDefault != null) {
            GroupAupEntity aup = new GroupAupEntity();
            aup.setId(entity.getId());
            aup.setGroupEnrollmentConfiguration(entity);
            aup.setType(GroupAupTypeEnum.URL);
            aup.setUrl(aupDefault);
            entity.setAupEntity(aup);
        }
        create(entity);
        group.setSingleAttribute(Utils.DEFAULT_CONFIGURATION_NAME, entity.getId());
    }

    public void update(GroupEnrollmentConfigurationEntity entity, GroupEnrollmentConfigurationRepresentation rep) {
        toEntity(entity, rep, entity.getGroup().getId());
        update(entity);
    }

    public Stream<GroupEnrollmentConfigurationEntity> getGroupAdminGroups(String userId) {
        return em.createNamedQuery("getAdminGroups").setParameter("userId", userId).getResultStream();
    }

    public Stream<GroupEnrollmentConfigurationEntity> getByGroup(String groupId) {
        return em.createNamedQuery("getByGroup").setParameter("groupId", groupId).getResultStream();
    }

    public Stream<GroupEnrollmentConfigurationEntity> getAvailableByGroup(String groupId) {
        return em.createNamedQuery("getAvailableByGroup").setParameter("groupId", groupId).getResultStream();
    }

    private void toEntity(GroupEnrollmentConfigurationEntity entity, GroupEnrollmentConfigurationRepresentation rep, String groupId) {
        entity.setName(rep.getName());
        entity.setActive(rep.isActive());
        entity.setVisibleToNotMembers(rep.isVisibleToNotMembers());
        entity.setRequireApproval(rep.getRequireApproval());
        entity.setRequireApprovalForExtension(rep.getRequireApprovalForExtension());
        entity.setValidFrom(rep.getValidFrom());
        entity.setMembershipExpirationDays(rep.getMembershipExpirationDays());
        entity.setEnrollmentConclusion(rep.getEnrollmentConclusion());
        entity.setEnrollmentIntroduction(rep.getEnrollmentIntroduction());
        entity.setInvitationConclusion(rep.getInvitationConclusion());
        entity.setInvitationIntroduction(rep.getInvitationIntroduction());
        entity.setMultiselectRole(rep.getMultiselectRole());
        if (rep.getGroupRoles() != null) {
            entity.setGroupRoles(rep.getGroupRoles().stream().map(x -> {
                GroupRolesEntity r = groupRolesRepository.getGroupRolesByNameAndGroup(x, groupId);
                if (r != null) {
                    GroupRolesEntity role = new GroupRolesEntity();
                    role.setId(r.getId());
                    role.setGroup(r.getGroup());
                    role.setName(r.getName());
                    return role;
                } else {
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList()));
        } else {
            entity.setGroupRoles(null);
        }
        if (rep.getAup() != null) {
            entity.setAupEntity(toEntity(rep.getAup(), entity));
        } else {
            entity.setAupEntity(null);
        }
        entity.setCommentsNeeded(rep.getCommentsNeeded());
        if (rep.getCommentsNeeded()){
            entity.setCommentsLabel(rep.getCommentsLabel());
            entity.setCommentsDescription(rep.getCommentsDescription());
        } else {
            entity.setCommentsLabel(null);
            entity.setCommentsDescription(null);
        }

    }

    private GroupAupEntity toEntity(GroupAupRepresentation rep, GroupEnrollmentConfigurationEntity configuration) {
        GroupAupEntity entity = configuration.getAupEntity();
        if (entity == null) {
            entity = new GroupAupEntity();
            entity.setId(configuration.getId());
            entity.setGroupEnrollmentConfiguration(configuration);
        }

        entity.setType(rep.getType());
        entity.setContent(rep.getContent());
        entity.setMimeType(rep.getMimeType());
        entity.setUrl(rep.getUrl());
        return entity;
    }

    public void deleteByGroup(String groupId) {
        em.createNamedQuery("deleteGroupAupByEnrollmentConfiguration").setParameter("groupId", groupId).executeUpdate();
        em.createNamedQuery("deleteEnrollmentConfigurationByGroup").setParameter("groupId", groupId).executeUpdate();
    }

    public Stream<GroupModel> searchForGroupByNameStream(String search, Boolean exact, Integer first, Integer max) {
        TypedQuery<String> query;
        if (Boolean.TRUE.equals(exact)) {
            query = em.createNamedQuery("getGroupIdsByName", String.class);
        } else {
            query = em.createNamedQuery("getGroupIdsByNameContaining", String.class);
        }
        query.setParameter("realm", realm.getId()).setParameter("search", search);
        Stream<String> groups =  paginateQuery(query, first, max).getResultStream();

        return closing(paginateQuery(query, first, max).getResultStream().map(id -> session.groups().getGroupById(realm, id)).distinct());
    }

}
