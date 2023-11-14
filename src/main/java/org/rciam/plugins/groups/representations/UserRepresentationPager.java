package org.rciam.plugins.groups.representations;

import java.util.List;

import org.keycloak.representations.idm.UserRepresentation;

public class UserRepresentationPager {

    private List<UserRepresentation> results;
    private long count;

    public UserRepresentationPager(List<UserRepresentation> results, Long count) {
        this.results = results;
        this.count = count;
    }

    public List<UserRepresentation> getResults() {
        return results;
    }

    public void setResults(List<UserRepresentation> results) {
        this.results = results;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
