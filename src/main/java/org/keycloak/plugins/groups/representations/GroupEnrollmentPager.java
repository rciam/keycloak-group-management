package org.keycloak.plugins.groups.representations;

import java.util.List;

public class GroupEnrollmentPager {

    private List<GroupEnrollmentRepresentation> results;
    private long count;

    public GroupEnrollmentPager(List<GroupEnrollmentRepresentation> results, long count){
        this.results = results;
        this.count = count;
    }

    public List<GroupEnrollmentRepresentation> getResults() {
        return results;
    }

    public void setResults(List<GroupEnrollmentRepresentation> results) {
        this.results = results;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
