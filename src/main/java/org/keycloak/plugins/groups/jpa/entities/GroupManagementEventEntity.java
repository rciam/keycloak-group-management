package org.keycloak.plugins.groups.jpa.entities;

import java.time.LocalDate;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "GROUP_MANAGEMENT_EVENT")
public class GroupManagementEventEntity {

    @Id
    @Column(name = "ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @Column(name="DATE")
    private LocalDate date;

    @Column(name="DATE_FOR_WEEK_TASKS")
    private LocalDate dateForWeekTasks;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDate getDateForWeekTasks() {
        return dateForWeekTasks;
    }

    public void setDateForWeekTasks(LocalDate dateForWeekTasks) {
        this.dateForWeekTasks = dateForWeekTasks;
    }
}
