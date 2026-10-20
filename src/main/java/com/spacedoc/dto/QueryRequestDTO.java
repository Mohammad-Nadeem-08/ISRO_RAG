package com.spacedoc.dto;

public class QueryRequestDTO {
    private String query;
    private Integer topK;
    private String revisionId;
    private String sourceType;

    public QueryRequestDTO() {}

    public QueryRequestDTO(String query, Integer topK) {
        this.query = query;
        this.topK = topK;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }

    public String getRevisionId() { return revisionId; }
    public void setRevisionId(String revisionId) { this.revisionId = revisionId; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
}
