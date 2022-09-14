package org.keycloak.plugins.groups.representations;

import java.util.List;

public class UserVoGroupMembershipRepresentationPager {

    private List<UserVoGroupMembershipRepresentation> results;
    private long count;

    public UserVoGroupMembershipRepresentationPager(List<UserVoGroupMembershipRepresentation> results, long count){
        this.results = results;
        this.count = count;
    }

    public List<UserVoGroupMembershipRepresentation> getResults() {
        return results;
    }

    public void setResults(List<UserVoGroupMembershipRepresentation> results) {
        this.results = results;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
