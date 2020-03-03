package org.camunda.bpm.extension.hooks.services.analytics;


import org.apache.commons.lang3.StringUtils;

import java.util.List;

public interface IQueryFactory {

     /**
     * Helper method to retrieve data row from data source.
     * This method has made a strong assumption of using pid as primary key.
     *
     * @param tableName
     * @return
     */
    static String getValidationQuery(String tableName) {
        return new StringBuilder().append("SELECT * FROM ")
                .append(tableName)
                .append(" WHERE pid = :pid").toString();
    }

    /**
     * Helper Method to prepare dynamic queries
     * Supported operations are INSERT and UPDATE.
     *
     * This method has made a strong assumption of using pid as primary key.
     * @param tableName
     * @param columns
     * @param doesExists
     * @return
     */
    static String prepareQuery(String tableName, List<String> columns, Boolean doesExists) {
        StringBuilder query = new StringBuilder();
        StringBuilder colBinder = new StringBuilder();
        StringBuilder valuesBinder = new StringBuilder();
        for(String entry :columns) {
            if(!doesExists) {
                colBinder.append(entry+ " ,");
                valuesBinder.append(":"+entry+" ,");
            } else {
                colBinder.append(entry+ " = :"+entry+ " ,");
            }
        }
        if(!doesExists) {
            query.append("INSERT INTO "+tableName+" (");
            query.append(StringUtils.removeEnd(colBinder.toString(),","));
            query.append(" ) values (");
            query.append(StringUtils.removeEnd(valuesBinder.toString(),","));
            query.append(")");
        } else {
            query.append("UPDATE "+tableName+" SET ");
            query.append(StringUtils.removeEnd(colBinder.toString(),","));
            query.append(" WHERE pid = :pid");
        }
        return query.toString();
    }

    /**
     * Helper method to validate the entity name
     *
     * @param tableName
     * @return
     */
    static Boolean validateEntityKey(String tableName) {
        if(StringUtils.isEmpty(tableName)) {return Boolean.FALSE;}
       return Boolean.TRUE;
    }

}
