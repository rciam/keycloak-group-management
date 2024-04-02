package org.rciam.plugins.groups.representations;

import java.util.List;

public class UserGroupMembershipExtensionRepresentationPager {

    private List<UserGroupMembershipExtensionRepresentation> results;
    private long count;

    public UserGroupMembershipExtensionRepresentationPager(List<UserGroupMembershipExtensionRepresentation> results, long count){
        this.results = results;
        this.count = count;
    }

    public List<UserGroupMembershipExtensionRepresentation> getResults() {
        return results;
    }

    public void setResults(List<UserGroupMembershipExtensionRepresentation> results) {
        this.results = results;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
