package com.github.b3kt.application.helper;

import com.github.b3kt.application.dto.PageRequest;

public class QueryFilterBuilder {
    
    private StringBuilder queryString;
    private Object[] params;
    private int paramIndex;

    public QueryFilterBuilder() {
        this.queryString = new StringBuilder("1=1");
        this.params = new Object[0];
        this.paramIndex = 1;
    }

    public QueryFilterBuilder withSearch(String searchField, String... additionalFields) {
        if (searchField != null && !searchField.isEmpty()) {
            String searchPattern = "%" + searchField.toLowerCase() + "%";
            StringBuilder searchClause = new StringBuilder(" and (lower(");
            searchClause.append(additionalFields[0]).append(") like ?").append(paramIndex);
            for (int i = 1; i < additionalFields.length; i++) {
                searchClause.append(" or lower(").append(additionalFields[i]).append(") like ?").append(paramIndex);
            }
            searchClause.append(")");
            
            addParam(searchPattern);
        }
        return this;
    }

    public QueryFilterBuilder withSearch(String search) {
        if (search != null && !search.isEmpty()) {
            String searchPattern = "%" + search.toLowerCase() + "%";
            queryString.append(" and (lower(noSpk) like ?").append(paramIndex)
                    .append(" or lower(nopol) like ?").append(paramIndex)
                    .append(" or lower(namaKaryawan) like ?").append(paramIndex)
                    .append(" or lower(namaPelanggan) like ?").append(paramIndex)
                    .append(")");
            addParam(searchPattern);
        }
        return this;
    }

    public QueryFilterBuilder withStatusFilter(String statusFilter, String fieldName) {
        if (statusFilter != null && !statusFilter.isEmpty()) {
            String[] statuses = statusFilter.split(",");
            StringBuilder statusQuery = new StringBuilder(" and (");
            
            for (int i = 0; i < statuses.length; i++) {
                if (i > 0) {
                    statusQuery.append(" or ");
                }
                statusQuery.append(fieldName).append(" = ?").append(paramIndex);
                addParam(statuses[i].trim().toUpperCase());
            }
            statusQuery.append(")");
            queryString.append(statusQuery);
        }
        return this;
    }

    public QueryFilterBuilder withDateRange(String startDate, String endDate, String dateField) {
        if (startDate != null && !startDate.isEmpty()) {
            queryString.append(" and ").append(dateField).append(" >= ?").append(paramIndex);
            addParam(startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            queryString.append(" and ").append(dateField).append(" <= ?").append(paramIndex);
            addParam(endDate + " 23:59:59");
        }
        return this;
    }

    private void addParam(Object value) {
        Object[] newParams = new Object[params.length + 1];
        System.arraycopy(params, 0, newParams, 0, params.length);
        newParams[params.length] = value;
        params = newParams;
        paramIndex++;
    }

    public String getQueryString() {
        return queryString.toString();
    }

    public Object[] getParams() {
        return params;
    }

    public static QueryFilterBuilder create() {
        return new QueryFilterBuilder();
    }
}
