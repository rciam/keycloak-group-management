package org.rciam.plugins.groups.representations;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema
public class GroupEnrollmentRequestPager {

    private List<GroupEnrollmentRequestRepresentation> results;
    private long count;

    public GroupEnrollmentRequestPager(List<GroupEnrollmentRequestRepresentation> results, long count){
        this.results = results;
        this.count = count;
    }

    public List<GroupEnrollmentRequestRepresentation> getResults() {
        return results;
    }

    public void setResults(List<GroupEnrollmentRequestRepresentation> results) {
        this.results = results;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
